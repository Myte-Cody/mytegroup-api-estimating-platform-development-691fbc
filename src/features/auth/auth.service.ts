import { Injectable, UnauthorizedException, ForbiddenException, BadRequestException } from '@nestjs/common';
import * as argon2 from 'argon2';
import * as crypto from 'crypto';
import { AuditLogService } from '../../common/services/audit-log.service';
import { ActorContext, UsersService } from '../users/users.service';
import { OrganizationsService } from '../organizations/organizations.service';
import { RegisterDto } from './dto/register.dto';
import { VerifyEmailDto } from './dto/verify-email.dto';
import { ForgotPasswordDto } from './dto/forgot-password.dto';
import { ResetPasswordDto } from './dto/reset-password.dto';
import { EmailService } from '../email/email.service';
import { Role } from '../../common/roles';
import { STRONG_PASSWORD_REGEX } from './dto/password-rules';
import zxcvbn from 'zxcvbn';
import { WaitlistService } from '../waitlist/waitlist.service';
import { normalizeDomainFromEmail } from '../../common/utils/domain.util';
@Injectable()
export class AuthService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly users: UsersService,
    private readonly orgs: OrganizationsService,
    private readonly email: EmailService,
    private readonly waitlist: WaitlistService
  ) {}

  private generateToken(ttlMs: number) {
    const token = crypto.randomBytes(32).toString('hex');
    const hash = crypto.createHash('sha256').update(token).digest('hex');
    const expires = new Date(Date.now() + ttlMs);
    return { token, hash, expires };
  }

  async login(email: string, pass: string) {
    const user = await this.users.findByEmail(email);
    const isValid = user && (await argon2.verify(user.passwordHash, pass));
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

    const updated = await this.users.markLastLogin(user.id);
    return this.sanitizeUser(updated || user);
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

  async listUsers(orgId?: string, actor?: ActorContext) {
    const resolvedActor = actor || { orgId };
    return this.users.list(resolvedActor, { orgId });
  }

  async register(dto: RegisterDto) {
    if (!STRONG_PASSWORD_REGEX.test(dto.password)) {
      throw new BadRequestException('Password does not meet strength requirements');
    }
    if (!dto.legalAccepted) {
      throw new BadRequestException('You must accept the Privacy Policy and Terms & Conditions to create an account');
    }
    const normalizedEmail = dto.email.toLowerCase();
    const domain = normalizeDomainFromEmail(normalizedEmail);
    if (!domain) throw new BadRequestException('Invalid email domain');

    const firstName = (dto.firstName || '').trim() || undefined;
    const lastName = (dto.lastName || '').trim() || undefined;
    const derivedUsername =
      (dto.username || '').trim() ||
      [firstName, lastName].filter(Boolean).join(' ') ||
      normalizedEmail.split('@')[0];

    const waitlistEntry: any = await this.waitlist.findByEmail(normalizedEmail);
    const inviteGateEnabled = this.waitlist.shouldEnforceInviteGate();
    if (inviteGateEnabled) {
      if (!waitlistEntry || !['invited', 'activated'].includes(waitlistEntry?.status || '')) {
        await this.waitlist.logEvent('waitlist.register_blocked', {
          email: normalizedEmail,
          status: waitlistEntry?.status || 'missing',
        });
        throw new ForbiddenException(
          'Invite required. Request early access, verify your email + phone, and we will invite your company in a wave.'
        );
      }

      const fullyVerified = waitlistEntry?.verifyStatus === 'verified' && waitlistEntry?.phoneVerifyStatus === 'verified';
      if (!fullyVerified) {
        await this.waitlist.logEvent('waitlist.register_blocked_unverified', {
          email: normalizedEmail,
          verifyStatus: waitlistEntry?.verifyStatus,
          phoneVerifyStatus: waitlistEntry?.phoneVerifyStatus,
        });
        throw new ForbiddenException('Please verify your email and phone before creating your account.');
      }

      const inviteToken = (dto as any).inviteToken;
      if (!inviteToken) {
        throw new ForbiddenException('Invite link required. Please use the invite email to register.');
      }
      const inviteHash = crypto.createHash('sha256').update(inviteToken).digest('hex');
      const tokenExpiresAt = waitlistEntry?.inviteTokenExpiresAt ? new Date(waitlistEntry.inviteTokenExpiresAt) : null;
      const tokenExpired = tokenExpiresAt ? tokenExpiresAt.getTime() < Date.now() : false;
      if (!waitlistEntry?.inviteTokenHash || waitlistEntry.inviteTokenHash !== inviteHash || tokenExpired) {
        throw new ForbiddenException('Invite link invalid or expired. Please request a fresh invite.');
      }
    }

    let domainLocked = false;
    if (this.waitlist.domainGateEnabled()) {
      const existingDomainOrg = await this.orgs.findByDomain(domain);
      if (existingDomainOrg) {
        await this.waitlist.logEvent('waitlist.register_blocked_domain', {
          email: normalizedEmail,
          domain,
          orgId: (existingDomainOrg as any)._id || (existingDomainOrg as any).id,
        });
        throw new ForbiddenException('Your company already has access. Please ask your org admin to invite you.');
      }
      const claimed = await this.waitlist.acquireDomainClaim(domain);
      domainLocked = claimed;
      if (!claimed) {
        await this.waitlist.logEvent('waitlist.register_blocked_domain_race', { email: normalizedEmail, domain });
        throw new ForbiddenException('Your company already has access. Please ask your org admin to invite you.');
      }
    }

    try {
      let organizationId = dto.organizationId;
      let createdOrgId: string | undefined;
      if (!organizationId) {
        const orgName = dto.organizationName || `${derivedUsername}'s Organization`;
        const org = await this.orgs.create({ name: orgName, primaryDomain: domain });
        organizationId = org.id || (org as any)._id;
        createdOrgId = organizationId;
      } else {
        await this.orgs.findById(organizationId);
      }

      let user;
      const emailVerification = inviteGateEnabled ? null : this.generateToken(1000 * 60 * 60 * 24); // 24h
      const assignedRole = createdOrgId ? Role.OrgOwner : Role.User;
      user = await this.users.create({
        username: derivedUsername,
        firstName,
        lastName,
        email: normalizedEmail,
        password: dto.password,
        organizationId,
        role: assignedRole,
        isOrgOwner: assignedRole === Role.OrgOwner,
        verificationTokenHash: emailVerification?.hash ?? null,
        verificationTokenExpires: emailVerification?.expires ?? null,
        isEmailVerified: inviteGateEnabled,
      }, undefined, { enforceSeat: true });

      const userId = (user as any).id || (user as any)._id;

      if (createdOrgId) {
        await this.orgs.setOwner(createdOrgId, userId);
      }
      await this.waitlist.markActivated(normalizedEmail, waitlistEntry?.cohortTag);
      await this.audit.log({
        eventType: 'auth.register',
        userId,
        orgId: organizationId,
      });
      if (emailVerification) {
        await this.email.sendVerificationEmail(normalizedEmail, emailVerification.token, organizationId, dto.username);
      }
      return user;
    } catch (err) {
      throw err;
    } finally {
      // ensure lock doesn't linger if creation failed silently
      if (domainLocked) {
        await this.waitlist.releaseDomainClaim(domain);
      }
    }
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
    await this.email.sendPasswordResetEmail(user.email, token, user.organizationId as string, user.username);
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
