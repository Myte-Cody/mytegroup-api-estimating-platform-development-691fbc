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
import { SeatsService } from '../seats/seats.service';
import { seatConfig } from '../../config/app.config';

@Injectable()
export class InvitesService {
  constructor(
    @InjectModel('Invite') private readonly inviteModel: Model<Invite>,
    private readonly audit: AuditLogService,
    private readonly users: UsersService,
    private readonly email: EmailService,
    private readonly contacts: ContactsService,
    private readonly seats: SeatsService
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
    const email = (dto.email || '').trim().toLowerCase();
    const existing = await this.inviteModel.findOne({
      orgId,
      email,
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
      email,
      createdAt: { $gte: throttleSince },
    });
    if (recentCount > 0) {
      throw new BadRequestException('Invite recently sent to this email; please wait before re-inviting');
    }
    const { token, hash, expires } = this.generateToken(dto.expiresInHours || 72);
    const invite = await this.inviteModel.create({
      orgId,
      email,
      role: dto.role || Role.User,
      contactId: dto.contactId || null,
      tokenHash: hash,
      tokenExpires: expires,
      status: 'pending',
      createdByUserId: actorId,
    });
    await this.email.sendInviteEmail(email, token, orgId);
    await this.audit.log({
      eventType: 'invite.created',
      orgId,
      userId: actorId,
      entity: 'Invite',
      entityId: invite.id,
      metadata: { email, role: dto.role },
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

    await this.seats.ensureOrgSeats(invite.orgId, seatConfig.defaultSeatsPerOrg);

    const session = await this.inviteModel.db.startSession();
    let createdUser: any = null;
    let inviteContactId: string | null = invite.contactId || null;
    let invitedUserId: string | null = null;

    try {
      session.startTransaction();

      const inviteQuery: any = this.inviteModel.findOne({ _id: invite._id });
      if (typeof inviteQuery?.session === 'function') {
        inviteQuery.session(session);
      }
      const inviteTx = await (typeof inviteQuery?.exec === 'function' ? inviteQuery.exec() : inviteQuery);
      if (!inviteTx) throw new NotFoundException('Invite not found');
      if (inviteTx.status === 'accepted') throw new BadRequestException('Invite already accepted');
      if (inviteTx.status === 'expired') throw new BadRequestException('Invite expired');
      if (inviteTx.tokenExpires < new Date()) throw new BadRequestException('Invite expired');
      if (inviteTx.archivedAt) throw new ForbiddenException('Invite unavailable');

      createdUser = await this.users.create(
        {
          username: dto.username,
          email: inviteTx.email,
          password: dto.password,
          organizationId: inviteTx.orgId,
          role: (inviteTx.role as Role) || Role.User,
          isEmailVerified: true,
        },
        undefined,
        { session, enforceSeat: true }
      );

      const rawUserId = (createdUser as any).id || (createdUser as any)._id || null;
      invitedUserId = rawUserId ? String(rawUserId) : null;
      inviteTx.status = 'accepted';
      inviteTx.acceptedAt = new Date();
      inviteTx.invitedUserId = invitedUserId;
      inviteTx.tokenHash = null;
      await inviteTx.save({ session });

      await this.audit.log(
        {
          eventType: 'invite.accepted',
          orgId: inviteTx.orgId,
          userId: invitedUserId || undefined,
          entity: 'Invite',
          entityId: inviteTx.id,
          metadata: { contactId: inviteTx.contactId },
        },
        { session }
      );

      await session.commitTransaction();
    } catch (err) {
      await session.abortTransaction();
      throw err;
    } finally {
      session.endSession();
    }

    if (inviteContactId && invitedUserId) {
      try {
        await this.contacts.linkInvitedUser(invite.orgId, inviteContactId, invitedUserId);
      } catch {
        // non-fatal: user accepted invite and has a seat; contact linkage can be repaired later
      }
    }

    return { user: createdUser };
  }
}
