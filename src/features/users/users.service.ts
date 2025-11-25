import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as bcrypt from 'bcryptjs';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateUserDto } from './dto/create-user.dto';
import { Role } from '../../common/roles';
import { User } from './schemas/user.schema';
import { STRONG_PASSWORD_REGEX } from '../auth/dto/password-rules';

@Injectable()
export class UsersService {
  constructor(
    @InjectModel('User') private readonly userModel: Model<User>,
    private readonly audit: AuditLogService
  ) {}

  private sanitize(user: any) {
    const { passwordHash, ...rest } = user.toObject ? user.toObject() : user;
    return rest;
  }

  async create(dto: CreateUserDto) {
    const existing = await this.userModel.findOne({ email: dto.email });
    if (existing) throw new ConflictException('Email already registered');
    if (!dto.organizationId) throw new ConflictException('organizationId is required');
    if (!STRONG_PASSWORD_REGEX.test(dto.password)) {
      throw new BadRequestException('Password does not meet strength requirements');
    }

    const passwordHash = await bcrypt.hash(dto.password, 10);
    const user = await this.userModel.create({
      username: dto.username,
      email: dto.email,
      passwordHash,
      role: dto.role || Role.User,
      organizationId: dto.organizationId,
      isEmailVerified: dto.isEmailVerified ?? false,
      verificationTokenHash: dto.verificationTokenHash ?? null,
      verificationTokenExpires: dto.verificationTokenExpires ?? null,
      resetTokenHash: dto.resetTokenHash ?? null,
      resetTokenExpires: dto.resetTokenExpires ?? null,
      isOrgOwner: dto.isOrgOwner ?? false,
    });
    await this.audit.log({
      eventType: 'user.created',
      userId: user.id,
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
    return this.userModel.find({ organizationId: orgId, archivedAt: null });
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

  async archiveUser(targetUserId: string, actor: { role?: string; orgId?: string }) {
    const user = await this.userModel.findById(targetUserId);
    if (!user) throw new NotFoundException('User not found');
    const isSuper = actor.role === Role.SuperAdmin;
    if (!isSuper && actor.orgId && user.organizationId !== actor.orgId) {
      throw new ForbiddenException('Cannot archive user outside your organization');
    }
    if (user.archivedAt) return this.sanitize(user);
    user.archivedAt = new Date();
    await user.save();
    await this.audit.log({
      eventType: 'user.archived',
      userId: user.id,
      orgId: user.organizationId,
      entity: 'User',
      entityId: user.id,
    });
    return this.sanitize(user);
  }

  async unarchiveUser(targetUserId: string, actor: { role?: string; orgId?: string }) {
    const user = await this.userModel.findById(targetUserId);
    if (!user) throw new NotFoundException('User not found');
    const isSuper = actor.role === Role.SuperAdmin;
    if (!isSuper && actor.orgId && user.organizationId !== actor.orgId) {
      throw new ForbiddenException('Cannot unarchive user outside your organization');
    }
    if (!user.archivedAt) return this.sanitize(user);
    user.archivedAt = null;
    await user.save();
    await this.audit.log({
      eventType: 'user.unarchived',
      userId: user.id,
      orgId: user.organizationId,
      entity: 'User',
      entityId: user.id,
    });
    return this.sanitize(user);
  }
}
