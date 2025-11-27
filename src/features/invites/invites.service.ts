import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as crypto from 'crypto';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateInviteDto } from './dto/create-invite.dto';
import { AcceptInviteDto } from './dto/accept-invite.dto';
import { Invite } from './schemas/invite.schema';
import { UsersService } from '../users/users.service';
import { EmailService } from '../email/email.service';
import { Role, canAssignRoles, mergeRoles } from '../../common/roles';
import { ContactsService } from '../contacts/contacts.service';

@Injectable()
export class InvitesService {
  constructor(
    @InjectModel('Invite') private readonly inviteModel: Model<Invite>,
    private readonly audit: AuditLogService,
    private readonly users: UsersService,
    private readonly email: EmailService,
    private readonly contacts: ContactsService
  ) {}

  private generateToken(hours: number) {
    const token = crypto.randomBytes(32).toString('hex');
    const hash = crypto.createHash('sha256').update(token).digest('hex');
    const expires = new Date(Date.now() + hours * 60 * 60 * 1000);
    return { token, hash, expires };
  }

  private validateInviteRole(actorRole: Role, requestedRole: Role) {
    if (requestedRole === Role.SuperAdmin) {
      throw new ForbiddenException('Cannot invite superadmin');
    }
    const actorRoles = mergeRoles(actorRole, []);
    const allowed = canAssignRoles(actorRoles, [requestedRole]);
    if (!allowed) {
      throw new ForbiddenException('Insufficient role to invite requested access');
    }
  }

  private async expireStale(orgId: string) {
    await this.inviteModel.updateMany(
      { orgId, status: 'pending', tokenExpires: { $lte: new Date() } },
      { status: 'expired' }
    );
  }

  async create(orgId: string, actorId: string, actorRole: Role, dto: CreateInviteDto) {
    this.validateInviteRole(actorRole, dto.role);
    await this.expireStale(orgId);
    const existing = await this.inviteModel.findOne({
      orgId,
      email: dto.email,
      status: 'pending',
      tokenExpires: { $gt: new Date() },
      archivedAt: null,
    });
    if (existing) {
      throw new BadRequestException('Pending invite already exists for this email');
    }
    const THROTTLE_WINDOW_MS = 10 * 60 * 1000; // 10 minutes
    const throttleSince = new Date(Date.now() - THROTTLE_WINDOW_MS);
    const recentCount = await this.inviteModel.countDocuments({
      orgId,
      email: dto.email,
      createdAt: { $gte: throttleSince },
    });
    if (recentCount > 0) {
      throw new BadRequestException('Invite recently sent to this email; please wait before re-inviting');
    }
    const { token, hash, expires } = this.generateToken(dto.expiresInHours || 72);
    const invite = await this.inviteModel.create({
      orgId,
      email: dto.email,
      role: dto.role || Role.User,
      contactId: dto.contactId || null,
      tokenHash: hash,
      tokenExpires: expires,
      status: 'pending',
      createdByUserId: actorId,
    });
    await this.email.sendInviteEmail(dto.email, token, orgId);
    await this.audit.log({
      eventType: 'invite.created',
      orgId,
      userId: actorId,
      entity: 'Invite',
      entityId: invite.id,
      metadata: { email: dto.email, role: dto.role },
    });
    if (dto.contactId) {
      await this.contacts.update(
        { userId: actorId, role: actorRole, orgId },
        orgId,
        dto.contactId,
        { inviteStatus: 'pending', invitedAt: new Date() }
      );
    }
    return invite.toObject ? invite.toObject() : invite;
  }

  async list(orgId: string) {
    await this.expireStale(orgId);
    return this.inviteModel.find({ orgId, archivedAt: null }).lean();
  }

  async resend(orgId: string, inviteId: string, actorId: string) {
    await this.expireStale(orgId);
    const invite = await this.inviteModel.findOne({ _id: inviteId, orgId, archivedAt: null });
    if (!invite) throw new NotFoundException('Invite not found');
    if (invite.status === 'accepted') throw new BadRequestException('Invite already accepted');
    const { token, hash, expires } = this.generateToken(72);
    invite.tokenHash = hash;
    invite.tokenExpires = expires;
    await invite.save();
    await this.email.sendInviteEmail(invite.email, token, orgId);
    await this.audit.log({
      eventType: 'invite.resent',
      orgId,
      userId: actorId,
      entity: 'Invite',
      entityId: invite.id,
    });
    return invite.toObject ? invite.toObject() : invite;
  }

  async accept(dto: AcceptInviteDto) {
    // Expire any pending invites past TTL before processing this token
    await this.inviteModel.updateMany(
      { status: 'pending', tokenExpires: { $lte: new Date() } },
      { status: 'expired' }
    );
    const tokenHash = crypto.createHash('sha256').update(dto.token).digest('hex');
    // search all invites (shared DB only) then fall back to tenants
    const invite = await this.inviteModel.findOne({ tokenHash, archivedAt: null });
    if (!invite) throw new NotFoundException('Invite not found or expired');
    if (invite.status === 'accepted') throw new BadRequestException('Invite already accepted');
    if (invite.tokenExpires < new Date()) {
      invite.status = 'expired';
      await invite.save();
      throw new BadRequestException('Invite expired');
    }
    if (invite.archivedAt) throw new ForbiddenException('Invite unavailable');

    const user = await this.users.create({
      username: dto.username,
      email: invite.email,
      password: dto.password,
      organizationId: invite.orgId,
      role: (invite.role as Role) || Role.User,
      isEmailVerified: true,
    });

    invite.status = 'accepted';
    invite.acceptedAt = new Date();
    invite.invitedUserId = (user as any).id || (user as any)._id;
    invite.tokenHash = null;
    await invite.save();

    if (invite.contactId) {
      await this.contacts.linkInvitedUser(invite.orgId, invite.contactId, invite.invitedUserId as string);
    }

    await this.audit.log({
      eventType: 'invite.accepted',
      orgId: invite.orgId,
      userId: invite.invitedUserId,
      entity: 'Invite',
      entityId: invite.id,
      metadata: { contactId: invite.contactId },
    });

    return { user };
  }
}
