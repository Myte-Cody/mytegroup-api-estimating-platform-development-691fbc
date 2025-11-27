import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as bcrypt from 'bcryptjs';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { Role, canAssignRoles, mergeRoles, resolvePrimaryRole } from '../../common/roles';
import { User } from './schemas/user.schema';
import { STRONG_PASSWORD_REGEX } from '../auth/dto/password-rules';

export type ActorContext = { role?: Role; roles?: Role[]; orgId?: string; id?: string };

@Injectable()
export class UsersService {
  constructor(
    @InjectModel('User') private readonly userModel: Model<User>,
    private readonly audit: AuditLogService
  ) {}

  private sanitize(user: any) {
    const {
      passwordHash,
      verificationTokenHash,
      verificationTokenExpires,
      resetTokenHash,
      resetTokenExpires,
      ...rest
    } = user.toObject ? user.toObject() : user;
    return rest;
  }

  private summarizeDiff(before: any, after: any, fields: string[]) {
    const changes: Record<string, { from: any; to: any }> = {};
    fields.forEach((field) => {
      if (before[field] === after[field]) return;
      changes[field] = { from: before[field], to: after[field] };
    });
    return changes;
  }

  private normalizeRoles(dto: Pick<CreateUserDto, 'role' | 'roles'>) {
    const roles = mergeRoles(dto.role as Role, dto.roles as Role[]);
    const primary = resolvePrimaryRole(roles);
    return { roles, primary };
  }

  private getActorRoles(actor: ActorContext) {
    return mergeRoles(actor.role as Role, actor.roles as Role[]);
  }

  private assertOrgScope(userOrgId: string | undefined | null, actor: ActorContext) {
    const actorRoles = this.getActorRoles(actor);
    const canBypass = actorRoles.includes(Role.SuperAdmin) || actorRoles.includes(Role.PlatformAdmin);
    if (canBypass) return;
    if (!actor.orgId) {
      throw new ForbiddenException('Missing organization context');
    }
    if (userOrgId && actor.orgId !== userOrgId) {
      throw new ForbiddenException('Cannot modify user outside your organization');
    }
  }

  private validateRoleChange(actor: ActorContext, roles: Role[]) {
    const normalizedRoles = mergeRoles(undefined, roles);
    if (!normalizedRoles.length) throw new BadRequestException('At least one role is required');
    const actorRoles = this.getActorRoles(actor);
    if (!actorRoles.length) {
      throw new ForbiddenException('Actor has no roles assigned');
    }
    if (!canAssignRoles(actorRoles, normalizedRoles)) {
      throw new ForbiddenException('Insufficient role to assign requested roles');
    }
    return normalizedRoles;
  }

  private resolveOrg(actor: ActorContext, requestedOrgId?: string) {
    const actorRoles = this.getActorRoles(actor);
    const privileged = actorRoles.includes(Role.SuperAdmin) || actorRoles.includes(Role.PlatformAdmin);
    const orgId = privileged && requestedOrgId ? requestedOrgId : actor.orgId;
    if (!orgId) throw new ForbiddenException('Missing organization context');
    return { orgId, actorRoles, privileged };
  }

  private canManageCompliance(actorRoles: Role[]) {
    return actorRoles.some((role) =>
      [
        Role.SuperAdmin,
        Role.PlatformAdmin,
        Role.Admin,
        Role.OrgOwner,
        Role.OrgAdmin,
        Role.Compliance,
        Role.ComplianceOfficer,
      ].includes(role)
    );
  }

  private assertNotLegalHold(user: any, action: string) {
    if (user.legalHold) {
      throw new ForbiddenException(`Cannot ${action} user on legal hold`);
    }
  }

  async create(dto: CreateUserDto, actor?: ActorContext) {
    const existing = await this.userModel.findOne({ email: dto.email });
    if (existing) throw new ConflictException('Email already registered');
    if (!dto.organizationId) throw new ConflictException('organizationId is required');
    if (!STRONG_PASSWORD_REGEX.test(dto.password)) {
      throw new BadRequestException('Password does not meet strength requirements');
    }
    const { roles, primary } = this.normalizeRoles(dto);
    if (actor) {
      this.validateRoleChange(actor, roles);
    }
    const passwordHash = await bcrypt.hash(dto.password, 10);
    const user = await this.userModel.create({
      username: dto.username,
      email: dto.email,
      passwordHash,
      role: primary,
      roles,
      organizationId: dto.organizationId,
      isEmailVerified: dto.isEmailVerified ?? false,
      verificationTokenHash: dto.verificationTokenHash ?? null,
      verificationTokenExpires: dto.verificationTokenExpires ?? null,
      resetTokenHash: dto.resetTokenHash ?? null,
      resetTokenExpires: dto.resetTokenExpires ?? null,
      isOrgOwner: dto.isOrgOwner ?? roles.includes(Role.OrgOwner),
      archivedAt: null,
      lastLogin: null,
      piiStripped: false,
      legalHold: false,
    });
    await this.audit.log({
      eventType: 'user.created',
      userId: actor?.id || user.id,
      orgId: user.organizationId,
      entity: 'User',
      entityId: user.id,
    });
    return this.sanitize(user);
  }

  async findByEmail(email: string) {
    return this.userModel.findOne({ email, archivedAt: null });
  }

  async scopedFindAll(orgId: string) {
    const users = await this.userModel.find({ organizationId: orgId, archivedAt: null });
    return users.map((u) => this.sanitize(u));
  }

  async list(
    actor: ActorContext,
    options: {
      orgId?: string;
      includeArchived?: boolean;
    } = {}
  ) {
    const { orgId } = this.resolveOrg(actor, options.orgId);
    const filter: any = { organizationId: orgId };
    if (!options.includeArchived) {
      filter.archivedAt = null;
    }
    const users = await this.userModel.find(filter);
    return users.map((u) => this.sanitize(u));
  }

  async getById(id: string, actor: ActorContext, includeArchived = false) {
    const user = await this.userModel.findById(id);
    if (!user) throw new NotFoundException('User not found');
    this.assertOrgScope(user.organizationId, actor);
    if (user.archivedAt && !includeArchived) {
      throw new NotFoundException('User not found');
    }
    return this.sanitize(user);
  }

  async findByVerificationToken(hash: string) {
    return this.userModel.findOne({
      verificationTokenHash: hash,
      verificationTokenExpires: { $gt: new Date() },
      archivedAt: null,
      legalHold: { $ne: true },
    });
  }

  async setVerificationToken(userId: string, hash: string, expires: Date) {
    return this.userModel.findByIdAndUpdate(
      userId,
      { verificationTokenHash: hash, verificationTokenExpires: expires, isEmailVerified: false },
      { new: true }
    );
  }

  async clearVerificationToken(userId: string) {
    return this.userModel.findByIdAndUpdate(
      userId,
      { verificationTokenHash: null, verificationTokenExpires: null, isEmailVerified: true },
      { new: true }
    );
  }

  async findByResetToken(hash: string) {
    return this.userModel.findOne({
      resetTokenHash: hash,
      resetTokenExpires: { $gt: new Date() },
      archivedAt: null,
      legalHold: { $ne: true },
    });
  }

  async setResetToken(userId: string, hash: string, expires: Date) {
    return this.userModel.findByIdAndUpdate(
      userId,
      { resetTokenHash: hash, resetTokenExpires: expires },
      { new: true }
    );
  }

  async clearResetTokenAndSetPassword(userId: string, newPassword: string) {
    const passwordHash = await bcrypt.hash(newPassword, 10);
    return this.userModel.findByIdAndUpdate(
      userId,
      {
        passwordHash,
        resetTokenHash: null,
        resetTokenExpires: null,
      },
      { new: true }
    );
  }

  async archiveUser(targetUserId: string, actor: ActorContext) {
    const user = await this.userModel.findById(targetUserId);
    if (!user) throw new NotFoundException('User not found');
    this.assertOrgScope(user.organizationId, actor);
    this.assertNotLegalHold(user, 'archive');
    if (user.archivedAt) return this.sanitize(user);
    user.archivedAt = new Date();
    await user.save();
    await this.audit.log({
      eventType: 'user.archived',
      userId: actor.id || user.id,
      orgId: user.organizationId,
      entity: 'User',
      entityId: user.id,
      metadata: { archivedAt: user.archivedAt },
    });
    return this.sanitize(user);
  }

  async unarchiveUser(targetUserId: string, actor: ActorContext) {
    const user = await this.userModel.findById(targetUserId);
    if (!user) throw new NotFoundException('User not found');
    this.assertOrgScope(user.organizationId, actor);
    this.assertNotLegalHold(user, 'unarchive');
    if (!user.archivedAt) return this.sanitize(user);
    user.archivedAt = null;
    await user.save();
    await this.audit.log({
      eventType: 'user.unarchived',
      userId: actor.id || user.id,
      orgId: user.organizationId,
      entity: 'User',
      entityId: user.id,
      metadata: { archivedAt: user.archivedAt },
    });
    return this.sanitize(user);
  }

  async getUserRoles(targetUserId: string, actor: ActorContext) {
    const user = await this.userModel.findById(targetUserId);
    if (!user) throw new NotFoundException('User not found');
    this.assertOrgScope(user.organizationId, actor);
    const sanitized = this.sanitize(user) as any;
    const roles = mergeRoles(sanitized.role as Role, sanitized.roles as Role[]);
    return { ...sanitized, roles, role: sanitized.role || resolvePrimaryRole(roles) };
  }

  async listOrgRoles(orgId: string | undefined, actor: ActorContext) {
    const actorRoles = this.getActorRoles(actor);
    const canBypass = actorRoles.includes(Role.SuperAdmin) || actorRoles.includes(Role.PlatformAdmin);
    const resolvedOrg = canBypass && orgId ? orgId : actor.orgId;
    if (!resolvedOrg) throw new ForbiddenException('Missing organization context');
    const users = await this.userModel.find({ organizationId: resolvedOrg, archivedAt: null });
    return users.map((u) => {
      const sanitized = this.sanitize(u) as any;
      const roles = mergeRoles(sanitized.role as Role, sanitized.roles as Role[]);
      return { ...sanitized, roles, role: sanitized.role || resolvePrimaryRole(roles) };
    });
  }

  async updateRoles(targetUserId: string, roles: Role[], actor: ActorContext) {
    const user = await this.userModel.findById(targetUserId);
    if (!user) throw new NotFoundException('User not found');
    this.assertOrgScope(user.organizationId, actor);
    const normalizedRoles = this.validateRoleChange(actor, roles);
    if (user.archivedAt) {
      throw new ForbiddenException('Cannot change roles for archived users');
    }
    if (user.legalHold) {
      throw new ForbiddenException('Cannot change roles while user is on legal hold');
    }
    user.roles = normalizedRoles;
    user.role = resolvePrimaryRole(normalizedRoles);
    user.isOrgOwner = normalizedRoles.includes(Role.OrgOwner);
    await user.save();
    await this.audit.log({
      eventType: 'user.roles_updated',
      userId: actor.id || user.id,
      orgId: user.organizationId,
      entity: 'User',
      entityId: user.id,
      metadata: { roles: normalizedRoles, actorRoles: this.getActorRoles(actor) },
    });
    return this.sanitize(user);
  }

  async update(id: string, dto: UpdateUserDto, actor: ActorContext) {
    const user = await this.userModel.findById(id);
    if (!user) throw new NotFoundException('User not found');
    this.assertOrgScope(user.organizationId, actor);
    if (user.archivedAt) {
      throw new ForbiddenException('Cannot update archived users');
    }
    const actorRoles = this.getActorRoles(actor);
    const canManageCompliance = this.canManageCompliance(actorRoles);
    if (user.legalHold && !canManageCompliance) {
      throw new ForbiddenException('User on legal hold');
    }
    const before = this.sanitize(user);

    if (dto.email && dto.email !== user.email) {
      const normalizedEmail = dto.email.toLowerCase();
      const duplicate = await this.userModel.findOne({ email: normalizedEmail });
      if (duplicate && duplicate.id !== id) {
        throw new ConflictException('Email already registered');
      }
      user.email = normalizedEmail;
    }
    if (dto.username !== undefined) user.username = dto.username;
    if (dto.isEmailVerified !== undefined) {
      if (!canManageCompliance) {
        throw new ForbiddenException('Insufficient role to change verification state');
      }
      user.isEmailVerified = dto.isEmailVerified;
    }
    if (dto.piiStripped !== undefined) {
      if (!canManageCompliance) throw new ForbiddenException('Insufficient role to change compliance fields');
      user.piiStripped = dto.piiStripped;
    }
    if (dto.legalHold !== undefined) {
      if (!canManageCompliance) throw new ForbiddenException('Insufficient role to change compliance fields');
      user.legalHold = dto.legalHold;
    }

    await user.save();
    const after = this.sanitize(user);
    await this.audit.log({
      eventType: 'user.updated',
      userId: actor.id || user.id,
      orgId: user.organizationId,
      entity: 'User',
      entityId: user.id,
      metadata: { changes: this.summarizeDiff(before, after, ['username', 'email', 'isEmailVerified', 'piiStripped', 'legalHold']) },
    });
    return after;
  }

  async markLastLogin(userId: string) {
    return this.userModel.findByIdAndUpdate(
      userId,
      { lastLogin: new Date() },
      { new: true, lean: true }
    );
  }
}
