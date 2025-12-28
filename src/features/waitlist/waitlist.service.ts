import { BadRequestException, ForbiddenException, Injectable, Logger, ServiceUnavailableException } from '@nestjs/common'
import { InjectModel } from '@nestjs/mongoose'
import { Model } from 'mongoose'
import * as crypto from 'crypto'
import { AuditLogService } from '../../common/services/audit-log.service'
import { buildClientUrl } from '../../config/app.config'
import { normalizeDomainFromEmail } from '../../common/utils/domain.util'
import { EmailService } from '../email/email.service'
import { StartWaitlistDto } from './dto/start-waitlist.dto'
import { VerifyWaitlistDto } from './dto/verify-waitlist.dto'
import { ResendWaitlistDto } from './dto/resend-waitlist.dto'
import { VerifyWaitlistPhoneDto } from './dto/verify-waitlist-phone.dto'
import { waitlistConfig } from './waitlist.config'
import { WaitlistEntry, WaitlistStatus, WaitlistVerifyStatus } from './waitlist.schema'
import { waitlistInviteTemplate, waitlistVerificationTemplate } from '../email/templates'
import { UsersService } from '../users/users.service'
import { createRedisClient } from '../../common/redis/redis.client'
import { RedisRateLimiter } from '../../common/redis/rate-limiter'
import { createQueue, createQueueScheduler, createWorker } from '../../queues/queue.factory'
import { opsConfig } from '../../config/ops.config'
import { SmsService } from '../sms/sms.service'

export type WaitlistStats = {
  waitlistCount: number
  waitlistDisplayCount: number
  freeSeatsPerOrg: number
  spotsLeftThisWave?: number
}

@Injectable()
export class WaitlistService {
  private readonly logger = new Logger(WaitlistService.name)
  private readonly redis = createRedisClient('waitlist')
  private readonly limiter = new RedisRateLimiter(this.redis, 'waitlist')
  private readonly domainDeny = new Set(waitlistConfig.domainPolicy.denylist || [])
  private readonly mailQueue = createQueue('waitlist-mail')
  private readonly mailScheduler = createQueueScheduler('waitlist-mail')
  private readonly mailDlq = createQueue('waitlist-mail-dlq')
  private readonly ops = opsConfig()
  private lastMailAlertAt = 0
  private mailWorkerStarted = false

  constructor(
    @InjectModel('Waitlist') private readonly model: Model<WaitlistEntry>,
    private readonly audit: AuditLogService,
    private readonly email: EmailService,
    private readonly sms: SmsService,
    private readonly users: UsersService
  ) {
    this.startMailWorker()
  }

  private normalizeEmail(email: string) {
    return (email || '').trim().toLowerCase()
  }

  private normalizePhone(phone: string) {
    return (phone || '').trim()
  }

  private isPhoneValid(phone: string) {
    return /^\+[1-9]\d{1,14}$/.test(phone)
  }

  private isEmailValid(email: string) {
    return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)
  }

  private async hasActiveUser(email: string) {
    try {
      const user = await this.users.findByEmail(email)
      return !!user
    } catch {
      return false
    }
  }

  private generateCode(length = waitlistConfig.verification.codeLength) {
    const digits = '0123456789'
    let code = ''
    for (let i = 0; i < length; i++) {
      code += digits[Math.floor(Math.random() * digits.length)]
    }
    return code
  }

  private computeExpiry(now = new Date()) {
    const ttl = waitlistConfig.verification.ttlMinutes || 30
    return new Date(now.getTime() + ttl * 60 * 1000)
  }

  private ensureDomainAllowed(email: string) {
    const domain = normalizeDomainFromEmail(email) || ''
    if (domain && this.domainDeny.has(domain)) {
      throw new ForbiddenException(
        'Please use your company email address (no personal providers like Gmail, Outlook, Yahoo, or iCloud).'
      )
    }
  }

  private async ensureNotBlocked(entry: WaitlistEntry) {
    if (!entry) return entry
    if (entry.verifyStatus !== 'blocked') return entry
    const now = new Date()
    if (entry.verifyBlockedUntil && entry.verifyBlockedUntil.getTime() > now.getTime()) {
      throw new ForbiddenException('Too many attempts. Please try later.')
    }
    // unblock after expiry
    const unblocked = await this.model
      .findOneAndUpdate(
        { _id: entry._id },
        {
          $set: {
            verifyStatus: 'unverified' as WaitlistVerifyStatus,
            verifyBlockedAt: null,
            verifyBlockedUntil: null,
            verifyAttempts: 0,
            verifyResends: 0,
          },
        },
        { new: true }
      )
      .lean()
    return unblocked || entry
  }

  private async ensurePhoneNotBlocked(entry: WaitlistEntry) {
    if (!entry) return entry
    if (entry.phoneVerifyStatus !== 'blocked') return entry
    const now = new Date()
    if (entry.phoneVerifyBlockedUntil && entry.phoneVerifyBlockedUntil.getTime() > now.getTime()) {
      throw new ForbiddenException('Too many attempts. Please try later.')
    }
    // unblock after expiry
    const unblocked = await this.model
      .findOneAndUpdate(
        { _id: entry._id },
        {
          $set: {
            phoneVerifyStatus: 'unverified' as WaitlistVerifyStatus,
            phoneVerifyBlockedAt: null,
            phoneVerifyBlockedUntil: null,
            phoneVerifyAttempts: 0,
            phoneVerifyResends: 0,
          },
        },
        { new: true }
      )
      .lean()
    return unblocked || entry
  }

  private async maybeBlock(entry: WaitlistEntry, reason: string) {
    const now = new Date()
    const until = new Date(now.getTime() + (waitlistConfig.verification.blockMinutes || 60) * 60 * 1000)
    await this.model.updateOne(
      { _id: entry._id },
      {
        $set: {
          verifyStatus: 'blocked' as WaitlistVerifyStatus,
          verifyBlockedAt: now,
          verifyBlockedUntil: until,
          verifyCode: null,
          verifyExpiresAt: null,
        },
      }
    )
    await this.audit.log({
      eventType: 'marketing.waitlist_blocked',
      actor: entry.email,
      metadata: { reason },
    })
    throw new ForbiddenException('Too many attempts. Please try later.')
  }

  private async maybeBlockPhone(entry: WaitlistEntry, reason: string) {
    const now = new Date()
    const until = new Date(now.getTime() + (waitlistConfig.verification.blockMinutes || 60) * 60 * 1000)
    await this.model.updateOne(
      { _id: entry._id },
      {
        $set: {
          phoneVerifyStatus: 'blocked' as WaitlistVerifyStatus,
          phoneVerifyBlockedAt: now,
          phoneVerifyBlockedUntil: until,
          phoneVerifyCode: null,
          phoneVerifyExpiresAt: null,
        },
      }
    )
    await this.audit.log({
      eventType: 'marketing.waitlist_blocked',
      actor: entry.email,
      metadata: { reason: `phone:${reason}` },
    })
    throw new ForbiddenException('Too many attempts. Please try later.')
  }

  private async assertTotalLimits(entry: WaitlistEntry) {
    const maxTotalAttempts = waitlistConfig.verification.maxTotalAttempts || 12
    const maxTotalResends = waitlistConfig.verification.maxTotalResends || 6
    if (entry.verifyAttemptTotal >= maxTotalAttempts) {
      await this.maybeBlock(entry, 'max_total_attempts')
    }
    if (entry.verifyResends >= maxTotalResends) {
      await this.maybeBlock(entry, 'max_total_resends')
    }
    if ((entry.phoneVerifyAttemptTotal || 0) >= maxTotalAttempts) {
      await this.maybeBlockPhone(entry, 'max_total_attempts')
    }
    if ((entry.phoneVerifyResends || 0) >= maxTotalResends) {
      await this.maybeBlockPhone(entry, 'max_total_resends')
    }
  }

  private stableJitter(dayIndex: number, range: number) {
    const raw = Math.sin(dayIndex + 1) * 10000
    const fraction = raw - Math.floor(raw)
    return Math.round((fraction * 2 - 1) * range)
  }

  private projectedDisplayCount(actual: number, now = new Date()) {
    const cfg = waitlistConfig.marketing
    if (typeof cfg.overrideDisplayCount === 'number') return cfg.overrideDisplayCount
    const start = cfg.campaignStart || now
    const daysSinceStart = Math.max(
      0,
      Math.floor((now.getTime() - new Date(start).getTime()) / (1000 * 60 * 60 * 24))
    )
    const slope = (cfg.targetCount - cfg.baselineCount) / Math.max(cfg.targetDays, 1)
    const projected = cfg.baselineCount + slope * Math.min(daysSinceStart, cfg.targetDays)
    const jitter = cfg.jitterRange ? this.stableJitter(daysSinceStart, cfg.jitterRange) : 0
    const display = Math.max(actual, Math.round(projected + jitter))
    return Math.min(display, cfg.targetCount)
  }

  private parseTimeToMinutes(value: string) {
    const [h, m] = value.split(':').map((v) => Number(v))
    return h * 60 + (Number.isFinite(m) ? m : 0)
  }

  private isWithinInviteWindow(now = new Date()) {
    const { start, end, timezone } = waitlistConfig.invite.window
    const zoned = new Date(now.toLocaleString('en-US', { timeZone: timezone }))
    const minutes = zoned.getHours() * 60 + zoned.getMinutes()
    const startMinutes = this.parseTimeToMinutes(start)
    const endMinutes = this.parseTimeToMinutes(end)
    return minutes >= startMinutes && minutes <= endMinutes
  }

  private async ensureRedis() {
    if ((this.redis as any).status === 'wait' || (this.redis as any).status === 'close') {
      try {
        await this.redis.connect()
      } catch (err) {
        if (process.env.NODE_ENV === 'production') {
          throw new ServiceUnavailableException('Service temporarily unavailable')
        }
        this.logger.error(`Redis unavailable: ${err?.message || err}`)
      }
    }
  }

  private contactVerifyStatusKey(email: string) {
    return `contact:verify:status:${email}`
  }

  private async isContactEmailVerified(email: string) {
    await this.ensureRedis()
    const status = await this.redis.get(this.contactVerifyStatusKey(email))
    return status === 'verified'
  }

  private verifyCodeKey(email: string) {
    return `waitlist:verify:code:${email}`
  }

  private verifyAttemptKey(email: string) {
    return `waitlist:verify:attempt:${email}`
  }

  private phoneVerifyCodeKey(email: string) {
    return `waitlist:verify:phone:code:${email}`
  }

  private phoneVerifyAttemptKey(email: string) {
    return `waitlist:verify:phone:attempt:${email}`
  }

  private domainClaimKey(domain: string) {
    return `waitlist:domain:claim:${domain}`
  }

  private statsCacheKey() {
    return 'waitlist:stats'
  }

  private async assertWaitlistAllowed(email: string, ip?: string) {
    await this.ensureRedis()
    const normalizedEmail = this.normalizeEmail(email)
    if (!this.isEmailValid(normalizedEmail)) {
      throw new BadRequestException('Invalid email')
    }
    const { windowMs, submissionsPerEmail, submissionsPerIp } = waitlistConfig.rateLimit
    const emailLimit = await this.limiter.allow(`submit:email:${normalizedEmail}`, submissionsPerEmail, windowMs)
    if (!emailLimit.allowed) {
      throw new ForbiddenException('Too many submissions for this email; please try again later.')
    }
    if (ip) {
      const ipLimit = await this.limiter.allow(`submit:ip:${ip}`, submissionsPerIp, windowMs)
      if (!ipLimit.allowed) {
        throw new ForbiddenException('Too many submissions from this source; please slow down.')
      }
    }
  }

  async assertEventAllowed(ip?: string) {
    if (!ip) return
    await this.ensureRedis()
    const { windowMs, eventsPerIp } = waitlistConfig.rateLimit
    const res = await this.limiter.allow(`event:ip:${ip}`, eventsPerIp, windowMs)
    if (!res.allowed) {
      throw new ForbiddenException('Rate limit exceeded')
    }
  }

  private async cacheStats(stats: WaitlistStats) {
    await this.ensureRedis()
    await this.redis.set(this.statsCacheKey(), JSON.stringify(stats), 'EX', 60)
  }

  private async getCachedStats(): Promise<WaitlistStats | null> {
    await this.ensureRedis()
    const raw = await this.redis.get(this.statsCacheKey())
    if (!raw) return null
    try {
      return JSON.parse(raw) as WaitlistStats
    } catch {
      return null
    }
  }

  private async storeVerificationCode(email: string, code: string, ttlMs: number) {
    await this.ensureRedis()
    await this.redis.set(this.verifyCodeKey(email), code, 'PX', ttlMs)
    await this.redis.del(this.verifyAttemptKey(email))
  }

  private async storePhoneVerificationCode(email: string, code: string, ttlMs: number) {
    await this.ensureRedis()
    await this.redis.set(this.phoneVerifyCodeKey(email), code, 'PX', ttlMs)
    await this.redis.del(this.phoneVerifyAttemptKey(email))
  }

  private async fetchVerificationCode(email: string) {
    await this.ensureRedis()
    return this.redis.get(this.verifyCodeKey(email))
  }

  private async fetchPhoneVerificationCode(email: string) {
    await this.ensureRedis()
    return this.redis.get(this.phoneVerifyCodeKey(email))
  }

  private async bumpVerifyAttempt(email: string, ttlMs: number) {
    await this.ensureRedis()
    const results = await this.redis.multi().incr(this.verifyAttemptKey(email)).pexpire(this.verifyAttemptKey(email), ttlMs, 'NX').exec()
    const count = Number(results?.[0]?.[1] ?? 0)
    return count
  }

  private async bumpPhoneVerifyAttempt(email: string, ttlMs: number) {
    await this.ensureRedis()
    const results = await this.redis
      .multi()
      .incr(this.phoneVerifyAttemptKey(email))
      .pexpire(this.phoneVerifyAttemptKey(email), ttlMs, 'NX')
      .exec()
    const count = Number(results?.[0]?.[1] ?? 0)
    return count
  }

  private async clearVerificationState(email: string) {
    await this.ensureRedis()
    await this.redis.del(this.verifyCodeKey(email), this.verifyAttemptKey(email))
  }

  private async clearPhoneVerificationState(email: string) {
    await this.ensureRedis()
    await this.redis.del(this.phoneVerifyCodeKey(email), this.phoneVerifyAttemptKey(email))
  }

  private startMailWorker() {
    if (this.mailWorkerStarted) return
    this.mailWorkerStarted = true
    this.mailScheduler.waitUntilReady().catch((err) => {
      this.logger.error(`[waitlist-mail] scheduler failed: ${err?.message || err}`)
    })
    const worker = createWorker<{
      type: 'verify' | 'invite';
      email: string;
      code?: string;
      name?: string;
      domain?: string;
      registerLink?: string;
      bcc?: string[];
    }>(
      'waitlist-mail',
      async (job) => {
        if ((job.data as any).forceFail) {
          throw new Error('Forced failure for ops test')
        }
        if (job.data.type === 'verify') {
          const t = waitlistVerificationTemplate({ code: job.data.code || '000000', userName: job.data.name })
          await this.email.sendMail({ email: job.data.email, subject: t.subject, text: t.text, html: t.html })
        } else {
          const registerLink = job.data.registerLink || buildClientUrl('/auth/register')
          const calendly = 'https://calendly.com/ahmed-mekallach/thought-exchange'
          const domain = job.data.domain || 'your company'
          const t = waitlistInviteTemplate({ registerLink, domain, calendly })
          await this.email.sendMail({
            email: job.data.email,
            subject: t.subject,
            text: t.text,
            html: t.html,
            bcc: Array.isArray(job.data.bcc) ? job.data.bcc : undefined,
          })
        }
      }
    )
    worker.on('failed', (job, err) => {
      this.logger.error(`[waitlist-mail] job ${job?.id} failed: ${err?.message || err}`)
      if (job) {
        const attempts = job.opts.attempts || 1
        if (job.attemptsMade >= attempts) {
          this.mailDlq
            .add(
              'failed-mail',
              { data: job.data, failedReason: err?.message || 'unknown' },
              { removeOnComplete: true, attempts: 1 }
            )
            .then(() => job.remove().catch(() => undefined))
            .catch((e) => this.logger.error(`[waitlist-mail] failed to enqueue DLQ: ${e?.message || e}`))
        }
      }
      this.maybeSendQueueAlert().catch((e) => this.logger.error(`[waitlist-mail] alert failed: ${e?.message || e}`))
    })
  }

  private async maybeSendQueueAlert() {
    if (!this.ops.alertEmail) return
    const now = Date.now()
    if (now - this.lastMailAlertAt < this.ops.queueAlertDebounceMs) return
    const counts = await this.mailQueue.getJobCounts('waiting', 'failed')
    const dlqFailed = await this.mailDlq.getFailed(0, 2)
    const backlog = counts.waiting || 0
    const failed = counts.failed || 0
    const shouldAlert = backlog > this.ops.queueAlertThreshold || failed > 0 || (dlqFailed?.length || 0) > 0
    if (!shouldAlert) return

    const subject = `[ALERT] waitlist-mail queue issues`
    const bodyText = `Queue waitlist-mail has waiting=${backlog}, failed=${failed}, dlq=${dlqFailed.length || 0}`
    await this.email.sendMail({
      email: this.ops.alertEmail,
      subject,
      text: bodyText,
      html: `<p>${bodyText}</p>`,
    })
    await this.audit.log({
      eventType: 'ops.queue_alert',
      metadata: { queue: 'waitlist-mail', waiting: backlog, failed, dlq: dlqFailed.length || 0 },
    })
    this.lastMailAlertAt = now
  }

  async start(dto: StartWaitlistDto, ip?: string) {
    await this.assertWaitlistAllowed(dto.email, ip)
    const now = new Date()
    const normalizedEmail = this.normalizeEmail(dto.email)
    const normalizedPhone = this.normalizePhone(dto.phone)
    if (!this.isPhoneValid(normalizedPhone)) {
      throw new BadRequestException('Invalid phone (expected E.164 format, e.g. +15145551234)')
    }

    this.ensureDomainAllowed(normalizedEmail)

    if (await this.hasActiveUser(normalizedEmail)) {
      await this.audit.log({
        eventType: 'marketing.waitlist_skip_active',
        actor: normalizedEmail,
      })
      return { status: 'ok', entry: null }
    }

    const existing = await this.model.findOne({ email: normalizedEmail, archivedAt: null })
    if (existing) {
      await this.ensureNotBlocked(existing)
      await this.ensurePhoneNotBlocked(existing as any)
      await this.assertTotalLimits(existing)
    }

    const contactVerified = await this.isContactEmailVerified(normalizedEmail)
    const emailAlreadyVerified = !!contactVerified || existing?.verifyStatus === 'verified'

    const existingPhone = this.normalizePhone((existing as any)?.phone || '')
    const phoneAlreadyVerified =
      existing?.phoneVerifyStatus === 'verified' && existingPhone === normalizedPhone

    if (emailAlreadyVerified) {
      await this.clearVerificationState(normalizedEmail)
    }
    if (phoneAlreadyVerified) {
      await this.clearPhoneVerificationState(normalizedEmail)
    }

    const needsEmailCode = !emailAlreadyVerified
    const needsPhoneCode = !phoneAlreadyVerified
    const emailCode = needsEmailCode ? this.generateCode() : null
    const phoneCode = needsPhoneCode ? this.generateCode() : null
    const expires = needsEmailCode || needsPhoneCode ? this.computeExpiry(now) : null
    const ttlMs = expires ? expires.getTime() - now.getTime() : 0

    const existingStatus = existing?.status as WaitlistStatus | undefined
    const status: WaitlistStatus =
      existingStatus === 'invited' || existingStatus === 'activated' ? existingStatus : 'pending-cohort'

    const entry = await this.model
      .findOneAndUpdate(
        { email: normalizedEmail, archivedAt: null },
        {
          $set: {
            name: dto.name,
            phone: normalizedPhone,
            role: dto.role,
            source: dto.source || 'landing',
            preCreateAccount: !!dto.preCreateAccount,
            marketingConsent: !!dto.marketingConsent,
            status,
            verifyStatus: emailAlreadyVerified ? ('verified' as WaitlistVerifyStatus) : ('unverified' as WaitlistVerifyStatus),
            verifyCode: emailCode,
            verifyExpiresAt: emailCode ? expires : null,
            verifyAttempts: 0,
            verifiedAt: emailAlreadyVerified ? (existing?.verifiedAt || now) : null,
            lastVerifySentAt: emailCode ? now : existing?.lastVerifySentAt || null,
            phoneVerifyStatus: phoneAlreadyVerified ? ('verified' as WaitlistVerifyStatus) : ('unverified' as WaitlistVerifyStatus),
            phoneVerifyCode: phoneCode,
            phoneVerifyExpiresAt: phoneCode ? expires : null,
            phoneVerifyAttempts: 0,
            phoneVerifiedAt: phoneAlreadyVerified ? ((existing as any)?.phoneVerifiedAt || now) : null,
            phoneLastVerifySentAt: phoneCode ? now : (existing as any)?.phoneLastVerifySentAt || null,
            updatedAt: now,
          },
          $setOnInsert: {
            email: normalizedEmail,
            createdAt: now,
            verifyResends: 0,
            verifyAttemptTotal: 0,
            phoneVerifyResends: 0,
            phoneVerifyAttemptTotal: 0,
            inviteFailureCount: 0,
          },
        },
        { upsert: true, new: true }
      )
      .lean()

    await this.audit.log({
      eventType: 'marketing.waitlist_submit',
      actor: normalizedEmail,
      metadata: {
        role: dto.role,
        source: dto.source || 'landing',
        preCreateAccount: !!dto.preCreateAccount,
        marketingConsent: !!dto.marketingConsent,
        needsEmailVerification: needsEmailCode,
        needsPhoneVerification: needsPhoneCode,
      },
    })

    if (emailCode && expires) {
      await this.storeVerificationCode(normalizedEmail, emailCode, ttlMs)
      await this.mailQueue.add(
        'verify',
        { type: 'verify', email: normalizedEmail, code: emailCode, name: dto.name },
        { removeOnComplete: true, removeOnFail: true, attempts: 3, backoff: { type: 'exponential', delay: 2000 } }
      )
    }

    if (phoneCode && expires) {
      await this.storePhoneVerificationCode(normalizedEmail, phoneCode, ttlMs)
      const ttl = waitlistConfig.verification.ttlMinutes || 30
      await this.sms.sendSms(normalizedPhone, `MYTE: Your verification code is ${phoneCode}. Expires in ${ttl} minutes.`)
    }

    if (!needsEmailCode && !needsPhoneCode) return { status: 'verified', entry }
    if (needsEmailCode && needsPhoneCode) return { status: 'verification_sent', entry }
    if (needsEmailCode) return { status: 'email_verification_sent', entry }
    return { status: 'phone_verification_sent', entry }
  }

  async verify(dto: VerifyWaitlistDto) {
    const normalizedEmail = this.normalizeEmail(dto.email)
    const entry = await this.model.findOne({ email: normalizedEmail, archivedAt: null })
    if (!entry) throw new BadRequestException('Not on the waitlist yet')
    await this.ensureNotBlocked(entry)
    await this.assertTotalLimits(entry)
    if (entry.verifyStatus === 'verified') return entry.toObject ? entry.toObject() : entry

    const now = new Date()
    const maxAttempts = waitlistConfig.verification.maxAttempts || 5
    const code = await this.fetchVerificationCode(normalizedEmail)
    if (!code || !entry.verifyExpiresAt || entry.verifyExpiresAt.getTime() < now.getTime()) {
      await this.audit.log({
        eventType: 'marketing.waitlist_verify_expired',
        actor: normalizedEmail,
      })
      throw new BadRequestException('Verification code expired. Please request a new code.')
    }

    const attemptCount = await this.bumpVerifyAttempt(normalizedEmail, entry.verifyExpiresAt.getTime() - now.getTime())
    if (attemptCount > maxAttempts) {
      await this.model.findOneAndUpdate({ _id: entry._id }, { $inc: { verifyAttemptTotal: 1 } })
      await this.maybeBlock(entry, 'max_attempts_per_code')
    }

    if ((code || '').trim() !== dto.code.trim()) {
      const updated = await this.model.findOneAndUpdate(
        { _id: entry._id },
        { $inc: { verifyAttempts: 1, verifyAttemptTotal: 1 } },
        { new: true }
      )
      if (updated) {
        await this.assertTotalLimits(updated as any)
      }
      await this.audit.log({
        eventType: 'marketing.waitlist_verify_invalid',
        actor: normalizedEmail,
        metadata: { attemptCount },
      })
      throw new BadRequestException('Invalid verification code')
    }

    await this.clearVerificationState(normalizedEmail)

    const updated = await this.model
      .findOneAndUpdate(
        { _id: entry._id },
        {
          $set: {
            verifyStatus: 'verified' as WaitlistVerifyStatus,
            verifiedAt: now,
            verifyCode: null,
            verifyExpiresAt: null,
            verifyAttempts: 0,
            status: (entry.status as WaitlistStatus) || ('pending-cohort' as WaitlistStatus),
          },
        },
        { new: true }
      )
      .lean()

    await this.audit.log({
      eventType: 'marketing.waitlist_verified',
      actor: normalizedEmail,
    })

    return updated
  }

  async verifyPhone(dto: VerifyWaitlistPhoneDto) {
    const normalizedEmail = this.normalizeEmail(dto.email)
    const entry = await this.model.findOne({ email: normalizedEmail, archivedAt: null })
    if (!entry) throw new BadRequestException('Not on the waitlist yet')
    await this.ensurePhoneNotBlocked(entry as any)
    await this.assertTotalLimits(entry)
    if (entry.phoneVerifyStatus === 'verified') return entry.toObject ? entry.toObject() : entry

    const now = new Date()
    const maxAttempts = waitlistConfig.verification.maxAttempts || 5
    const code = await this.fetchPhoneVerificationCode(normalizedEmail)
    if (!code || !entry.phoneVerifyExpiresAt || entry.phoneVerifyExpiresAt.getTime() < now.getTime()) {
      await this.audit.log({
        eventType: 'marketing.waitlist_phone_verify_expired',
        actor: normalizedEmail,
      })
      throw new BadRequestException('Verification code expired. Please request a new code.')
    }

    const attemptCount = await this.bumpPhoneVerifyAttempt(
      normalizedEmail,
      entry.phoneVerifyExpiresAt.getTime() - now.getTime()
    )
    if (attemptCount > maxAttempts) {
      await this.model.findOneAndUpdate({ _id: entry._id }, { $inc: { phoneVerifyAttemptTotal: 1 } })
      await this.maybeBlockPhone(entry as any, 'max_attempts_per_code')
    }

    if ((code || '').trim() !== dto.code.trim()) {
      const updated = await this.model.findOneAndUpdate(
        { _id: entry._id },
        { $inc: { phoneVerifyAttempts: 1, phoneVerifyAttemptTotal: 1 } },
        { new: true }
      )
      if (updated) {
        await this.assertTotalLimits(updated as any)
      }
      await this.audit.log({
        eventType: 'marketing.waitlist_phone_verify_invalid',
        actor: normalizedEmail,
        metadata: { attemptCount },
      })
      throw new BadRequestException('Invalid verification code')
    }

    await this.clearPhoneVerificationState(normalizedEmail)

    const updated = await this.model
      .findOneAndUpdate(
        { _id: entry._id },
        {
          $set: {
            phoneVerifyStatus: 'verified' as WaitlistVerifyStatus,
            phoneVerifiedAt: now,
            phoneVerifyCode: null,
            phoneVerifyExpiresAt: null,
            phoneVerifyAttempts: 0,
          },
        },
        { new: true }
      )
      .lean()

    await this.audit.log({
      eventType: 'marketing.waitlist_phone_verified',
      actor: normalizedEmail,
    })

    return updated
  }

  async resend(dto: ResendWaitlistDto, ip?: string) {
    const normalizedEmail = this.normalizeEmail(dto.email)
    const windowMs = waitlistConfig.rateLimit.windowMs
    const maxResends = waitlistConfig.verification.maxResendsPerWindow || 3
    await this.ensureRedis()
    const byEmail = await this.limiter.allow(`resend:${normalizedEmail}`, maxResends, windowMs)
    if (!byEmail.allowed) throw new ForbiddenException('Too many resend attempts. Please wait a moment.')
    if (ip) {
      const byIp = await this.limiter.allow(`resend:${normalizedEmail}:${ip}`, maxResends, windowMs)
      if (!byIp.allowed) throw new ForbiddenException('Too many resend attempts. Please wait a moment.')
    }
    const entry = await this.model.findOne({ email: normalizedEmail, archivedAt: null })
    if (!entry) throw new BadRequestException('Not on the waitlist yet')
    await this.ensureNotBlocked(entry)
    await this.assertTotalLimits(entry)
    if (entry.verifyStatus === 'verified') return { status: 'verified' }

    const now = new Date()
    const cooldownMs = (waitlistConfig.verification.resendCooldownMinutes || 2) * 60 * 1000
    if (entry.lastVerifySentAt && now.getTime() - new Date(entry.lastVerifySentAt).getTime() < cooldownMs) {
      throw new ForbiddenException('Please wait a bit before requesting another code.')
    }

    const code = this.generateCode()
    const expires = this.computeExpiry(now)
    const ttlMs = expires.getTime() - now.getTime()

    await this.model.updateOne(
      { _id: entry._id },
      {
        $set: {
          verifyCode: code,
          verifyExpiresAt: expires,
          verifyAttempts: 0,
          lastVerifySentAt: now,
        },
        $inc: { verifyResends: 1 },
      }
    )

    const refreshed = await this.model.findOne({ _id: entry._id })
    if (refreshed) {
      await this.assertTotalLimits(refreshed)
    }

    await this.audit.log({
      eventType: 'marketing.waitlist_verification_resent',
      actor: normalizedEmail,
    })

    await this.storeVerificationCode(normalizedEmail, code, ttlMs)

    await this.mailQueue.add(
      'verify',
      { type: 'verify', email: normalizedEmail, code, name: entry.name },
      { removeOnComplete: true, removeOnFail: true, attempts: 3, backoff: { type: 'exponential', delay: 2000 } }
    )

    return { status: 'sent' }
  }

  async resendPhone(dto: ResendWaitlistDto, ip?: string) {
    const normalizedEmail = this.normalizeEmail(dto.email)
    const windowMs = waitlistConfig.rateLimit.windowMs
    const maxResends = waitlistConfig.verification.maxResendsPerWindow || 3
    await this.ensureRedis()
    const byEmail = await this.limiter.allow(`resend-phone:${normalizedEmail}`, maxResends, windowMs)
    if (!byEmail.allowed) throw new ForbiddenException('Too many resend attempts. Please wait a moment.')
    if (ip) {
      const byIp = await this.limiter.allow(`resend-phone:${normalizedEmail}:${ip}`, maxResends, windowMs)
      if (!byIp.allowed) throw new ForbiddenException('Too many resend attempts. Please wait a moment.')
    }
    const entry = await this.model.findOne({ email: normalizedEmail, archivedAt: null })
    if (!entry) throw new BadRequestException('Not on the waitlist yet')
    await this.ensurePhoneNotBlocked(entry as any)
    await this.assertTotalLimits(entry)
    if (entry.phoneVerifyStatus === 'verified') return { status: 'verified' }

    const now = new Date()
    const cooldownMs = (waitlistConfig.verification.resendCooldownMinutes || 2) * 60 * 1000
    if (entry.phoneLastVerifySentAt && now.getTime() - new Date(entry.phoneLastVerifySentAt).getTime() < cooldownMs) {
      throw new ForbiddenException('Please wait a bit before requesting another code.')
    }

    const phone = this.normalizePhone((entry as any).phone)
    if (!this.isPhoneValid(phone)) {
      throw new BadRequestException('Phone missing or invalid. Please rejoin the waitlist with a valid phone number.')
    }

    const code = this.generateCode()
    const expires = this.computeExpiry(now)
    const ttlMs = expires.getTime() - now.getTime()

    await this.model.updateOne(
      { _id: entry._id },
      {
        $set: {
          phoneVerifyCode: code,
          phoneVerifyExpiresAt: expires,
          phoneVerifyAttempts: 0,
          phoneLastVerifySentAt: now,
        },
        $inc: { phoneVerifyResends: 1 },
      }
    )

    const refreshed = await this.model.findOne({ _id: entry._id })
    if (refreshed) {
      await this.assertTotalLimits(refreshed as any)
    }

    await this.audit.log({
      eventType: 'marketing.waitlist_phone_verification_resent',
      actor: normalizedEmail,
    })

    await this.storePhoneVerificationCode(normalizedEmail, code, ttlMs)

    const ttl = waitlistConfig.verification.ttlMinutes || 30
    await this.sms.sendSms(phone, `MYTE: Your verification code is ${code}. Expires in ${ttl} minutes.`)

    return { status: 'sent' }
  }

  async findByEmail(email: string) {
    return this.model.findOne({ email: this.normalizeEmail(email), archivedAt: null }).lean()
  }

  async stats(): Promise<WaitlistStats> {
    const cached = await this.getCachedStats()
    if (cached) return cached

    const waitlistCount = await this.model.countDocuments({ archivedAt: null })
    const waitlistDisplayCount = this.projectedDisplayCount(waitlistCount)

    let spotsLeftThisWave: number | undefined
    const waveSize = waitlistConfig.invite.batchLimit
    const cohortTag = waitlistConfig.invite.cohortTag
    if (waveSize && cohortTag) {
      const invitedInWave = await this.model.countDocuments({
        archivedAt: null,
        cohortTag,
        status: { $in: ['invited', 'activated'] },
      })
      const remaining = waveSize - invitedInWave
      spotsLeftThisWave = remaining > 0 ? remaining : 0
    }

    const stats = {
      waitlistCount,
      waitlistDisplayCount,
      freeSeatsPerOrg: waitlistConfig.marketing.freeSeatsPerOrg,
      spotsLeftThisWave,
    }
    await this.cacheStats(stats)
    return stats
  }

  async logEvent(event: string, meta?: Record<string, any>) {
    await this.audit.log({
      eventType: 'marketing.event',
      metadata: { event, ...(meta || {}) },
    })
  }

  async list(params: {
    status?: string
    verifyStatus?: string
    cohortTag?: string
    emailContains?: string
    page?: number
    limit?: number
  }) {
    const { status, verifyStatus, cohortTag, emailContains, page = 1, limit = 25 } = params || {}
    const query: any = { archivedAt: null }
    if (status) query.status = status
    if (verifyStatus) query.verifyStatus = verifyStatus
    if (cohortTag) query.cohortTag = cohortTag
    if (emailContains) query.email = new RegExp(emailContains, 'i')

    const skip = (page - 1) * limit
    const [data, total] = await Promise.all([
      this.model.find(query).sort({ createdAt: -1 }).skip(skip).limit(limit).lean(),
      this.model.countDocuments(query),
    ])

    return { data, total, page, limit }
  }

  private isFullyVerified(entry: WaitlistEntry) {
    return entry.verifyStatus === 'verified' && entry.phoneVerifyStatus === 'verified'
  }

  private assertFullyVerified(entry: WaitlistEntry) {
    const phone = this.normalizePhone((entry as any).phone || '')
    if (!this.isPhoneValid(phone)) {
      throw new ForbiddenException('Valid phone required before an invite can be issued.')
    }
    if (!this.isFullyVerified(entry)) {
      throw new ForbiddenException('Please verify your email and phone to lock your spot.')
    }
  }

  private hashToken(token: string) {
    return crypto.createHash('sha256').update(token).digest('hex')
  }

  private generateInviteToken(ttlHours = 24 * 7) {
    const token = crypto.randomBytes(32).toString('hex')
    const hash = this.hashToken(token)
    const expires = new Date(Date.now() + ttlHours * 60 * 60 * 1000)
    return { token, hash, expires }
  }

  private inviteSendMode() {
    return (waitlistConfig.invite.sendMode || 'token') as 'bcc' | 'token'
  }

  private inviteRequiresToken() {
    if (waitlistConfig.invite.requireToken !== undefined) {
      return !!waitlistConfig.invite.requireToken
    }
    return this.inviteSendMode() === 'token'
  }

  private resolveBatchToEmail() {
    return (
      waitlistConfig.invite.batchToEmail ||
      this.ops.alertEmail ||
      'waitlist@mytegroup.com'
    )
  }

  private buildRegisterLink(email?: string, inviteToken?: string) {
    const params = new URLSearchParams()
    if (email) params.set('email', email)
    if (inviteToken) params.set('invite', inviteToken)
    const qs = params.toString()
    const base = buildClientUrl('/auth/register')
    return qs ? `${base}?${qs}` : base
  }

  async markInvited(email: string, cohortTag?: string) {
    const normalizedEmail = this.normalizeEmail(email)
    const entry = await this.model.findOne({ email: normalizedEmail, archivedAt: null })
    if (!entry) {
      this.logger.warn(`No waitlist entry found to mark invited for ${email}`)
      return null
    }

    this.assertFullyVerified(entry as any)
    const requireToken = this.inviteRequiresToken()
    const tokenData = requireToken ? this.generateInviteToken() : null
    try {
      await this.sendInviteEmail(normalizedEmail, tokenData?.token || null)
    } catch (err) {
      await this.model.updateOne({ _id: entry._id }, { $inc: { inviteFailureCount: 1 } })
      throw err
    }

    const invited = await this.model
      .findOneAndUpdate(
        { _id: entry._id },
        {
          $set: {
            status: 'invited' as WaitlistStatus,
            invitedAt: new Date(),
            cohortTag: cohortTag || waitlistConfig.invite.cohortTag,
            verifyCode: null,
            verifyExpiresAt: null,
            phoneVerifyCode: null,
            phoneVerifyExpiresAt: null,
            inviteTokenHash: tokenData?.hash || null,
            inviteTokenExpiresAt: tokenData?.expires || null,
          },
        },
        { new: true }
      )
      .lean()

    await this.audit.log({
      eventType: 'marketing.waitlist_invited',
      actor: normalizedEmail,
      metadata: { cohortTag: cohortTag || waitlistConfig.invite.cohortTag },
    })

    return invited
  }

  async markActivated(email: string, cohortTag?: string) {
    const normalizedEmail = this.normalizeEmail(email)
    return this.model.findOneAndUpdate(
      { email: normalizedEmail, archivedAt: null },
      {
        $set: {
          status: 'activated',
          activatedAt: new Date(),
          cohortTag: cohortTag || null,
          inviteTokenHash: null,
          inviteTokenExpiresAt: null,
        },
      },
      { new: true }
    )
  }

  private async sendInviteEmail(email: string, inviteToken?: string | null) {
    const registerLink = this.buildRegisterLink(email, inviteToken || undefined)
    const calendly = 'https://calendly.com/ahmed-mekallach/thought-exchange'
    const domain = normalizeDomainFromEmail(email) || 'your company'
    const t = waitlistInviteTemplate({ registerLink, domain, calendly })
    await this.mailQueue.add(
      'invite',
      { type: 'invite', email, domain, registerLink },
      { removeOnComplete: true, removeOnFail: true, attempts: 3, backoff: { type: 'exponential', delay: 2000 } }
    )
  }

  private async sendInviteBatchEmail(emails: string[]) {
    const bcc = (emails || []).map((e) => this.normalizeEmail(e)).filter(Boolean)
    if (!bcc.length) return
    const registerLink = this.buildRegisterLink()
    const domain = 'your company'
    await this.mailQueue.add(
      'invite',
      { type: 'invite', email: this.resolveBatchToEmail(), domain, registerLink, bcc },
      { removeOnComplete: true, removeOnFail: true, attempts: 3, backoff: { type: 'exponential', delay: 2000 } }
    )
  }

  private async sendVerificationEmail(email: string, code: string, userName?: string) {
    const t = waitlistVerificationTemplate({ code, userName })
    await this.email.sendMail({ email, subject: t.subject, text: t.text, html: t.html })
  }

  async processInviteBatch(limit = waitlistConfig.invite.batchLimit, cohortTag?: string) {
    const now = new Date()
    if (!this.isWithinInviteWindow(now)) {
      this.logger.debug('Skipping invite batch; outside invite window')
      return { invited: 0, skipped: true, reason: 'outside-window' }
    }

    const eligible = await this.model
      .find({
        status: 'pending-cohort',
        verifyStatus: 'verified',
        phoneVerifyStatus: 'verified',
        archivedAt: null,
        createdAt: { $lte: new Date(now.getTime() - waitlistConfig.invite.delayHours * 60 * 60 * 1000) },
      })
      .sort({ createdAt: 1 })
      .limit(limit)
      .lean()

    if (!eligible.length) return { invited: 0, skipped: false }

    const sendMode = this.inviteSendMode()
    const requireToken = this.inviteRequiresToken()
    const effectiveMode = sendMode === 'bcc' && !requireToken ? 'bcc' : 'token'

    if (effectiveMode === 'bcc') {
      const emails = eligible.map((entry) => entry.email)
      try {
        await this.sendInviteBatchEmail(emails)
      } catch (err) {
        await this.model.updateMany({ _id: { $in: eligible.map((entry) => entry._id) } }, { $inc: { inviteFailureCount: 1 } })
        this.logger.error(`Failed to send invite batch: ${(err as Error).message}`)
        return { invited: 0, skipped: false, reason: 'send_failed' }
      }

      await this.model.updateMany(
        { _id: { $in: eligible.map((entry) => entry._id) }, status: 'pending-cohort' },
        {
          $set: {
            status: 'invited',
            invitedAt: new Date(),
            cohortTag: cohortTag || waitlistConfig.invite.cohortTag,
            verifyCode: null,
            verifyExpiresAt: null,
            phoneVerifyCode: null,
            phoneVerifyExpiresAt: null,
            inviteTokenHash: null,
            inviteTokenExpiresAt: null,
          },
        }
      )

      for (const entry of eligible) {
        await this.audit.log({
          eventType: 'marketing.waitlist_invited',
          actor: entry.email,
          metadata: { cohortTag: cohortTag || waitlistConfig.invite.cohortTag, via: 'auto-batch', mode: 'bcc' },
        })
      }

      return { invited: eligible.length, skipped: false }
    }

    let invited = 0
    for (const entry of eligible) {
      try {
        const tokenData = requireToken ? this.generateInviteToken() : null
        await this.sendInviteEmail(entry.email, tokenData?.token || null)
        const updated = await this.model.findOneAndUpdate(
          { _id: entry._id, status: 'pending-cohort' },
          {
            $set: {
              status: 'invited',
              invitedAt: new Date(),
              cohortTag: cohortTag || waitlistConfig.invite.cohortTag,
              verifyCode: null,
              verifyExpiresAt: null,
              phoneVerifyCode: null,
              phoneVerifyExpiresAt: null,
              inviteTokenHash: tokenData?.hash || null,
              inviteTokenExpiresAt: tokenData?.expires || null,
            },
          },
          { new: true }
        )
        if (!updated) continue
        invited += 1
        await this.audit.log({
          eventType: 'marketing.waitlist_invited',
          actor: updated.email,
          metadata: { cohortTag: cohortTag || waitlistConfig.invite.cohortTag, via: 'auto-batch', mode: 'token' },
        })
      } catch (err) {
        await this.model.updateOne({ _id: entry._id }, { $inc: { inviteFailureCount: 1 } })
        this.logger.error(`Failed to send invite to ${entry.email}: ${(err as Error).message}`)
        continue
      }
    }
    return { invited, skipped: false }
  }

  shouldEnforceInviteGate() {
    return !!waitlistConfig.invite.enforceGate
  }

  requiresInviteToken() {
    return this.inviteRequiresToken()
  }

  domainGateEnabled() {
    return !!waitlistConfig.invite.domainGateEnabled
  }

  async acquireDomainClaim(domain: string) {
    await this.ensureRedis()
    const res = await this.redis.set(this.domainClaimKey(domain), '1', 'PX', 2 * 60 * 1000, 'NX')
    return !!res
  }

  async releaseDomainClaim(domain: string) {
    await this.ensureRedis()
    await this.redis.del(this.domainClaimKey(domain))
  }
}
