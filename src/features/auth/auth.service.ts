import { Injectable, UnauthorizedException, ForbiddenException, BadRequestException } from '@nestjs/common';
import * as bcrypt from 'bcryptjs';
import * as crypto from 'crypto';
import { AuditLogService } from '../../common/services/audit-log.service';
import { UsersService } from '../users/users.service';
import { OrganizationsService } from '../organizations/organizations.service';
import { RegisterDto } from './dto/register.dto';
import { VerifyEmailDto } from './dto/verify-email.dto';
import { ForgotPasswordDto } from './dto/forgot-password.dto';
import { ResetPasswordDto } from './dto/reset-password.dto';
import { EmailService } from '../email/email.service';
import { Role } from '../../common/roles';
import { STRONG_PASSWORD_REGEX } from './dto/password-rules';
import zxcvbn from 'zxcvbn';
@Injectable()
export class AuthService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly users: UsersService,
    private readonly orgs: OrganizationsService,
    private readonly email: EmailService
  ) {}

  private generateToken(ttlMs: number) {
    const token = crypto.randomBytes(32).toString('hex');
    const hash = crypto.createHash('sha256').update(token).digest('hex');
    const expires = new Date(Date.now() + ttlMs);
    return { token, hash, expires };
  }

  async login(email: string, pass: string) {
    const user = await this.users.findByEmail(email);
    const isValid = user && (await bcrypt.compare(pass, user.passwordHash));
    await this.audit.log({
      eventType: 'auth.login',
      userId: user?.id,
      orgId: user?.organizationId,
      metadata: { email, success: !!isValid },
    });
    if (!isValid) throw new UnauthorizedException();
    if (user.archivedAt) throw new ForbiddenException('User archived');
    if (user.legalHold) throw new ForbiddenException('User on legal hold');
    if (!user.isEmailVerified) throw new ForbiddenException('Email not verified');

    return this.sanitizeUser(user);
  }

  private sanitizeUser(user: any) {
    const { passwordHash, ...rest } = user.toObject ? user.toObject() : user;
    return rest;
  }

  passwordStrength(password: string) {
    const result = zxcvbn(password || '');
    return {
      score: result.score,
      crackTimesDisplay: result.crack_times_display,
      feedback: result.feedback,
      guessesLog10: result.guesses_log10,
    };
  }

  async listUsers(orgId?: string) {
    return this.users.scopedFindAll(orgId);
  }

  async register(dto: RegisterDto) {
    if (!STRONG_PASSWORD_REGEX.test(dto.password)) {
      throw new BadRequestException('Password does not meet strength requirements');
    }
    let organizationId = dto.organizationId;
    let createdOrgId: string | undefined;
    if (!organizationId) {
      const orgName = dto.organizationName || `${dto.username}'s Organization`;
      const org = await this.orgs.create({ name: orgName });
      organizationId = org.id || (org as any)._id;
      createdOrgId = organizationId;
    } else {
      await this.orgs.findById(organizationId);
    }
    const { token, hash, expires } = this.generateToken(1000 * 60 * 60 * 24); // 24h
    const assignedRole = createdOrgId ? Role.OrgOwner : Role.User;
    const user = await this.users.create({
      username: dto.username,
      email: dto.email,
      password: dto.password,
      organizationId,
      role: assignedRole,
      isOrgOwner: assignedRole === Role.OrgOwner,
      verificationTokenHash: hash,
      verificationTokenExpires: expires,
      isEmailVerified: false,
    });
    if (createdOrgId) {
      await this.orgs.setOwner(createdOrgId, (user as any).id || (user as any)._id);
    }
    await this.audit.log({
      eventType: 'auth.register',
      userId: (user as any).id || (user as any)._id,
      orgId: organizationId,
    });
    await this.email.sendVerificationEmail(dto.email, token);
    return user;
  }

  async verifyEmail(dto: VerifyEmailDto) {
    const hash = crypto.createHash('sha256').update(dto.token).digest('hex');
    const user = await this.users.findByVerificationToken(hash);
    if (!user) throw new BadRequestException('Invalid or expired token');
    await this.users.clearVerificationToken(user.id);
    await this.audit.log({
      eventType: 'auth.verify_email',
      userId: user.id,
      orgId: user.organizationId,
    });
    return this.sanitizeUser(user);
  }

  async forgotPassword(dto: ForgotPasswordDto) {
    const user = await this.users.findByEmail(dto.email);
    if (!user) return { status: 'ok' }; // do not leak existence
    if (user.legalHold || user.archivedAt) throw new ForbiddenException('User unavailable');
    const { token, hash, expires } = this.generateToken(1000 * 60 * 60); // 1h
    await this.users.setResetToken(user.id, hash, expires);
    await this.audit.log({
      eventType: 'auth.password_reset_requested',
      userId: user.id,
      orgId: user.organizationId,
    });
    await this.email.sendPasswordResetEmail(user.email, token);
    return { status: 'ok' };
  }

  async resetPassword(dto: ResetPasswordDto) {
    if (!STRONG_PASSWORD_REGEX.test(dto.newPassword)) {
      throw new BadRequestException('Password does not meet strength requirements');
    }
    const hash = crypto.createHash('sha256').update(dto.token).digest('hex');
    const user = await this.users.findByResetToken(hash);
    if (!user) throw new BadRequestException('Invalid or expired token');
    if (user.legalHold || user.archivedAt) throw new ForbiddenException('User unavailable');
    const updated = await this.users.clearResetTokenAndSetPassword(user.id, dto.newPassword);
    await this.audit.log({
      eventType: 'auth.password_reset',
      userId: user.id,
      orgId: user.organizationId,
    });
    return this.sanitizeUser(updated);
  }
}
