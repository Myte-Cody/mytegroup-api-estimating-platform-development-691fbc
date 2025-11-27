import { BadRequestException, ForbiddenException, Injectable, Logger, ServiceUnavailableException } from '@nestjs/common'
import { InjectModel } from '@nestjs/mongoose'
import { Model } from 'mongoose'
import { AuditLogService } from '../../common/services/audit-log.service'
import { buildClientUrl } from '../../config/app.config'
import { normalizeDomainFromEmail } from '../../common/utils/domain.util'
import { EmailService } from '../email/email.service'
import { StartWaitlistDto } from './dto/start-waitlist.dto'
import { VerifyWaitlistDto } from './dto/verify-waitlist.dto'
import { ResendWaitlistDto } from './dto/resend-waitlist.dto'
import { waitlistConfig } from './waitlist.config'
import { WaitlistEntry, WaitlistStatus, WaitlistVerifyStatus } from './waitlist.schema'
import { waitlistInviteTemplate, waitlistVerificationTemplate } from '../email/templates'
import { UsersService } from '../users/users.service'
import { createRedisClient } from '../../common/redis/redis.client'
import { RedisRateLimiter } from '../../common/redis/rate-limiter'
import { createQueue, createQueueScheduler, createWorker } from '../../queues/queue.factory'
import { opsConfig } from '../../config/ops.config'

export type WaitlistStats = {
  waitlistCount: number
  waitlistDisplayCount: number
  freeSeatsPerOrg: number
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
    private readonly users: UsersService
  ) {
    this.startMailWorker()
  }

  private normalizeEmail(email: string) {
    return (email || '').trim().toLowerCase()
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

  private async assertCaptcha(token?: string, ip?: string) {
    const cfg = waitlistConfig.captcha
    if (!cfg.enabled || cfg.provider === 'none') return
    if (cfg.requireToken && !token) {
      throw new ForbiddenException('Captcha required')
    }
    const secret =
      cfg.provider === 'turnstile' ? process.env.TURNSTILE_SECRET : cfg.provider === 'hcaptcha' ? process.env.HCAPTCHA_SECRET : undefined
    if (!secret) return // treat as disabled when no secret configured
    if (!token) {
      throw new ForbiddenException('Captcha required')
    }
    const url = cfg.provider === 'turnstile' ? 'https://challenges.cloudflare.com/turnstile/v0/siteverify' : 'https://hcaptcha.com/siteverify'
    const body = new URLSearchParams({ secret, response: token })
    if (ip) body.append('remoteip', ip)
    const res = await fetch(url, {
      method: 'POST',
      body,
    })
    const data = (await res.json()) as any
    if (!data?.success) {
      throw new ForbiddenException('Captcha verification failed')
    }
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

  private async assertTotalLimits(entry: WaitlistEntry) {
    const maxTotalAttempts = waitlistConfig.verification.maxTotalAttempts || 12
    const maxTotalResends = waitlistConfig.verification.maxTotalResends || 6
    if (entry.verifyAttemptTotal >= maxTotalAttempts) {
      await this.maybeBlock(entry, 'max_total_attempts')
    }
    if (entry.verifyResends >= maxTotalResends) {
      await this.maybeBlock(entry, 'max_total_resends')
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

  private verifyCodeKey(email: string) {
    return `waitlist:verify:code:${email}`
  }

  private verifyAttemptKey(email: string) {
    return `waitlist:verify:attempt:${email}`
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

  private async fetchVerificationCode(email: string) {
    await this.ensureRedis()
    return this.redis.get(this.verifyCodeKey(email))
  }

  private async bumpVerifyAttempt(email: string, ttlMs: number) {
    await this.ensureRedis()
    const results = await this.redis.multi().incr(this.verifyAttemptKey(email)).pexpire(this.verifyAttemptKey(email), ttlMs, 'NX').exec()
    const count = Number(results?.[0]?.[1] ?? 0)
    return count
  }

  private async clearVerificationState(email: string) {
    await this.ensureRedis()
    await this.redis.del(this.verifyCodeKey(email), this.verifyAttemptKey(email))
  }

  private startMailWorker() {
    if (this.mailWorkerStarted) return
    this.mailWorkerStarted = true
    this.mailScheduler.waitUntilReady().catch((err) => {
      this.logger.error(`[waitlist-mail] scheduler failed: ${err?.message || err}`)
    })
    const worker = createWorker<{ type: 'verify' | 'invite'; email: string; code?: string; name?: string; domain?: string }>(
      'waitlist-mail',
      async (job) => {
        if ((job.data as any).forceFail) {
          throw new Error('Forced failure for ops test')
        }
        if (job.data.type === 'verify') {
          const t = waitlistVerificationTemplate({ code: job.data.code || '000000', userName: job.data.name })
          await this.email.sendMail({ email: job.data.email, subject: t.subject, text: t.text, html: t.html })
        } else {
          const registerLink = buildClientUrl('/auth/register')
          const calendly = 'https://calendly.com/ahmed-mekallach/thought-exchange'
          const domain = job.data.domain || 'your company'
          const t = waitlistInviteTemplate({ registerLink, domain, calendly })
          await this.email.sendMail({ email: job.data.email, subject: t.subject, text: t.text, html: t.html })
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
    await this.assertCaptcha(dto.captchaToken, ip)
    await this.assertWaitlistAllowed(dto.email, ip)
    const now = new Date()
    const normalizedEmail = this.normalizeEmail(dto.email)
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
      await this.assertTotalLimits(existing)
    }

    const code = this.generateCode()
    const expires = this.computeExpiry(now)
    const ttlMs = expires.getTime() - now.getTime()
    const update = {
      name: dto.name,
      role: dto.role,
      source: dto.source || 'landing',
      preCreateAccount: !!dto.preCreateAccount,
      marketingConsent: !!dto.marketingConsent,
      status: 'pending-cohort' as WaitlistStatus,
      verifyStatus: 'unverified' as WaitlistVerifyStatus,
      verifyCode: code,
      verifyExpiresAt: expires,
      verifyAttempts: 0,
      verifiedAt: null,
      lastVerifySentAt: now,
      invitedAt: null,
      cohortTag: null,
      updatedAt: now,
    }
    const entry = await this.model
      .findOneAndUpdate(
        { email: normalizedEmail, archivedAt: null },
        {
          $set: update,
          $setOnInsert: {
            email: normalizedEmail,
            createdAt: now,
            verifyResends: 0,
            verifyAttemptTotal: 0,
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
      },
    })

    await this.storeVerificationCode(normalizedEmail, code, ttlMs)

    await this.mailQueue.add(
      'verify',
      { type: 'verify', email: normalizedEmail, code, name: dto.name },
      { removeOnComplete: true, removeOnFail: true, attempts: 3, backoff: { type: 'exponential', delay: 2000 } }
    )

    return { status: 'verification_sent', entry }
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
            status: 'pending-cohort' as WaitlistStatus,
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

  async resend(dto: ResendWaitlistDto, ip?: string) {
    await this.assertCaptcha(dto.captchaToken, ip)
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

  async ensurePending(email: string, fallbackName: string, fallbackRole = 'Pending', source = 'auth-register') {
    const normalizedEmail = this.normalizeEmail(email)
    const existing = await this.model.findOne({ email: normalizedEmail, archivedAt: null })
    if (existing) return existing.toObject ? existing.toObject() : existing
    const now = new Date()
    const entry = await this.model
      .findOneAndUpdate(
        { email: normalizedEmail },
        {
          $set: {
            name: fallbackName,
            role: fallbackRole,
            source,
            preCreateAccount: false,
            marketingConsent: true,
            status: 'pending-cohort' as WaitlistStatus,
            verifyStatus: 'verified' as WaitlistVerifyStatus,
            verifiedAt: now,
            verifyCode: null,
            verifyExpiresAt: null,
            verifyAttempts: 0,
            updatedAt: now,
          },
          $setOnInsert: { email: normalizedEmail, createdAt: now, verifyResends: 0, inviteFailureCount: 0 },
        },
        { upsert: true, new: true }
      )
      .lean()
    return entry
  }

  async findByEmail(email: string) {
    return this.model.findOne({ email: this.normalizeEmail(email), archivedAt: null }).lean()
  }

  async stats(): Promise<WaitlistStats> {
    const cached = await this.getCachedStats()
    if (cached) return cached

    const waitlistCount = await this.model.countDocuments({ archivedAt: null })
    const waitlistDisplayCount = this.projectedDisplayCount(waitlistCount)
    const stats = {
      waitlistCount,
      waitlistDisplayCount,
      freeSeatsPerOrg: waitlistConfig.marketing.freeSeatsPerOrg,
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

  private async ensureVerified(entry: WaitlistEntry) {
    if (entry.verifyStatus === 'verified') return entry
    const now = new Date()
    const updated = await this.model
      .findOneAndUpdate(
        { _id: entry._id },
        {
          $set: {
            verifyStatus: 'verified' as WaitlistVerifyStatus,
            verifiedAt: entry.verifiedAt || now,
            verifyCode: null,
            verifyExpiresAt: null,
            verifyAttempts: 0,
          },
        },
        { new: true }
      )
      .lean()
    return updated || entry
  }

  async markInvited(email: string, cohortTag?: string) {
    const normalizedEmail = this.normalizeEmail(email)
    const entry = await this.model.findOne({ email: normalizedEmail, archivedAt: null })
    if (!entry) {
      this.logger.warn(`No waitlist entry found to mark invited for ${email}`)
      return null
    }

    const verifiedEntry = await this.ensureVerified(entry)
    try {
      await this.sendInviteEmail(normalizedEmail)
    } catch (err) {
      await this.model.updateOne({ _id: verifiedEntry._id }, { $inc: { inviteFailureCount: 1 } })
      throw err
    }

    const invited = await this.model
      .findOneAndUpdate(
        { _id: verifiedEntry._id },
        {
          $set: {
            status: 'invited' as WaitlistStatus,
            invitedAt: new Date(),
            cohortTag: cohortTag || waitlistConfig.invite.cohortTag,
            verifyStatus: 'verified' as WaitlistVerifyStatus,
            verifyCode: null,
            verifyExpiresAt: null,
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
      { $set: { status: 'activated', activatedAt: new Date(), cohortTag: cohortTag || null } },
      { new: true }
    )
  }

  private async sendInviteEmail(email: string) {
    const registerLink = buildClientUrl('/auth/register')
    const calendly = 'https://calendly.com/ahmed-mekallach/thought-exchange'
    const domain = normalizeDomainFromEmail(email) || 'your company'
    const t = waitlistInviteTemplate({ registerLink, domain, calendly })
    await this.mailQueue.add(
      'invite',
      { type: 'invite', email, domain },
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
        archivedAt: null,
        createdAt: { $lte: new Date(now.getTime() - waitlistConfig.invite.delayHours * 60 * 60 * 1000) },
      })
      .sort({ createdAt: 1 })
      .limit(limit)
      .lean()

    let invited = 0
    for (const entry of eligible) {
      try {
        await this.sendInviteEmail(entry.email)
        const updated = await this.model.findOneAndUpdate(
          { _id: entry._id, status: 'pending-cohort' },
          {
            $set: {
              status: 'invited',
              invitedAt: new Date(),
              cohortTag: cohortTag || waitlistConfig.invite.cohortTag,
              verifyStatus: 'verified',
              verifyCode: null,
              verifyExpiresAt: null,
            },
          },
          { new: true }
        )
        if (!updated) continue
        invited += 1
        await this.audit.log({
          eventType: 'marketing.waitlist_invited',
          actor: updated.email,
          metadata: { cohortTag: cohortTag || waitlistConfig.invite.cohortTag, via: 'auto-batch' },
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
