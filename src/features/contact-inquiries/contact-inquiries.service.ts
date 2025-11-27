import { BadRequestException, ForbiddenException, Injectable, Logger } from '@nestjs/common'
import { InjectModel } from '@nestjs/mongoose'
import type { Model } from 'mongoose'

import { AuditLogService } from '../../common/services/audit-log.service'
import { createRedisClient } from '../../common/redis/redis.client'
import { RedisRateLimiter } from '../../common/redis/rate-limiter'
import { normalizeDomainFromEmail } from '../../common/utils/domain.util'
import { EmailService } from '../email/email.service'
import { waitlistConfig } from '../waitlist/waitlist.config'
import { WaitlistService } from '../waitlist/waitlist.service'
import { CreateContactInquiryDto } from './dto/create-contact-inquiry.dto'
import { ConfirmContactInquiryDto } from './dto/confirm-contact-inquiry.dto'
import { UpdateContactInquiryDto } from './dto/update-contact-inquiry.dto'
import { VerifyContactInquiryDto } from './dto/verify-contact-inquiry.dto'
import type { ContactInquiry } from './schemas/contact-inquiry.schema'

export type ContactInquiryListResult = {
  data: ContactInquiry[]
  total: number
  page: number
  limit: number
}

const PERSONAL_DOMAINS = new Set(waitlistConfig.domainPolicy.denylist || [])
const VERIFIED_STATUS = 'verified'
const VERIFIED_CACHE_MINUTES = 24 * 60 // keep verified emails warm for 24h

@Injectable()
export class ContactInquiriesService {
  private readonly redis = createRedisClient('contact-inquiry')
  private readonly limiter = new RedisRateLimiter(this.redis, 'contact-inquiry')
  private readonly logger = new Logger(ContactInquiriesService.name)
  private readonly verifiedTtlMs = VERIFIED_CACHE_MINUTES * 60 * 1000

  constructor(
    @InjectModel('ContactInquiry') private readonly model: Model<ContactInquiry>,
    private readonly audit: AuditLogService,
    private readonly email: EmailService,
    private readonly waitlist: WaitlistService
  ) {}

  private normalizeEmail(email: string) {
    return (email || '').trim().toLowerCase()
  }

  private isPersonalEmail(email: string) {
    const domain = normalizeDomainFromEmail(email)
    return domain ? PERSONAL_DOMAINS.has(domain) : false
  }

  private verifyCodeKey(email: string) {
    return `contact:verify:code:${email}`
  }

  private verifyStatusKey(email: string) {
    return `contact:verify:status:${email}`
  }

  private verifyAttemptKey(email: string) {
    return `contact:verify:attempt:${email}`
  }

  private verifyCooldownKey(email: string) {
    return `contact:verify:cooldown:${email}`
  }

  private generateCode(length = waitlistConfig.verification.codeLength || 6) {
    const digits = '0123456789'
    let code = ''
    for (let i = 0; i < length; i += 1) {
      code += digits[Math.floor(Math.random() * digits.length)]
    }
    return code
  }

  private async ensureRedis() {
    if ((this.redis as any).status === 'wait' || (this.redis as any).status === 'close') {
      await this.redis.connect()
    }
  }

  private async assertContactAllowed(email: string, ip?: string) {
    await this.ensureRedis()
    const normalizedEmail = (email || '').trim().toLowerCase()
    const { windowMs, submissionsPerEmail, submissionsPerIp } = waitlistConfig.rateLimit
    const emailLimit = await this.limiter.allow(
      `contact:email:${normalizedEmail}`,
      submissionsPerEmail,
      windowMs
    )
    if (!emailLimit.allowed) {
      throw new ForbiddenException('Too many contact requests for this email; please try again later.')
    }
    if (ip) {
      const ipLimit = await this.limiter.allow(`contact:ip:${ip}`, submissionsPerIp, windowMs)
      if (!ipLimit.allowed) {
        throw new ForbiddenException('Too many contact requests from this source; please slow down.')
      }
    }
  }

  private async assertCaptcha(token?: string, ip?: string) {
    const cfg = waitlistConfig.captcha
    if (!cfg.enabled || cfg.provider === 'none') return
    if (cfg.requireToken && !token) {
      throw new ForbiddenException('Captcha required')
    }
    const secret =
      cfg.provider === 'turnstile'
        ? process.env.TURNSTILE_SECRET
        : cfg.provider === 'hcaptcha'
        ? process.env.HCAPTCHA_SECRET
        : undefined
    if (!secret) return
    if (!token) {
      throw new ForbiddenException('Captcha required')
    }
    const url =
      cfg.provider === 'turnstile'
        ? 'https://challenges.cloudflare.com/turnstile/v0/siteverify'
        : 'https://hcaptcha.com/siteverify'
    const body = new URLSearchParams({ secret, response: token })
    if (ip) body.append('remoteip', ip)
    const res = await fetch(url, { method: 'POST', body })
    const data = (await res.json()) as any
    if (!data?.success) {
      throw new ForbiddenException('Captcha verification failed')
    }
  }

  private async storeVerificationCode(email: string, code: string) {
    await this.ensureRedis()
    const ttlMs = (waitlistConfig.verification.ttlMinutes || 30) * 60 * 1000
    await this.redis.set(this.verifyCodeKey(email), code, 'PX', ttlMs)
    await this.redis.set(this.verifyStatusKey(email), 'unverified', 'PX', this.verifiedTtlMs)
    await this.redis.del(this.verifyAttemptKey(email))
  }

  private async readVerificationCode(email: string) {
    await this.ensureRedis()
    return this.redis.get(this.verifyCodeKey(email))
  }

  private async bumpVerifyAttempt(email: string) {
    await this.ensureRedis()
    const ttlMs = (waitlistConfig.verification.ttlMinutes || 30) * 60 * 1000
    const results = await this.redis
      .multi()
      .incr(this.verifyAttemptKey(email))
      .pexpire(this.verifyAttemptKey(email), ttlMs, 'NX')
      .exec()
    return Number(results?.[0]?.[1] ?? 0)
  }

  private async markEmailVerified(email: string) {
    await this.ensureRedis()
    await this.redis
      .multi()
      .set(this.verifyStatusKey(email), VERIFIED_STATUS, 'PX', this.verifiedTtlMs)
      .del(this.verifyCodeKey(email), this.verifyAttemptKey(email))
      .exec()
  }

  private async isEmailVerified(email: string) {
    await this.ensureRedis()
    const status = await this.redis.get(this.verifyStatusKey(email))
    return status === VERIFIED_STATUS
  }

  async sendVerification(dto: VerifyContactInquiryDto, ip?: string) {
    if (dto.trap) return { status: 'ok' }
    const normalizedEmail = this.normalizeEmail(dto.email)
    const domain = normalizeDomainFromEmail(normalizedEmail)

    await this.assertCaptcha(dto.captchaToken, ip)
    await this.ensureRedis()

    const cooldownMs = (waitlistConfig.verification.resendCooldownMinutes || 2) * 60 * 1000
    const cooldownKey = this.verifyCooldownKey(normalizedEmail)
    const cooldownTtl = await this.redis.pttl(cooldownKey)
    if (cooldownTtl > 0) {
      throw new ForbiddenException('Please wait before requesting another code.')
    }

    const { windowMs, submissionsPerEmail, submissionsPerIp } = waitlistConfig.rateLimit
    const sendLimit = await this.limiter.allow(
      `contact:verify:email:${normalizedEmail}`,
      waitlistConfig.verification.maxResendsPerWindow || submissionsPerEmail,
      windowMs
    )
    if (!sendLimit.allowed) {
      throw new ForbiddenException('Too many verification requests. Please try again later.')
    }
    if (ip) {
      const ipLimit = await this.limiter.allow(
        `contact:verify:ip:${ip}`,
        waitlistConfig.verification.maxResendsPerWindow || submissionsPerIp,
        windowMs
      )
      if (!ipLimit.allowed) {
        throw new ForbiddenException('Too many verification requests from this source. Please slow down.')
      }
    }

    const code = this.generateCode()
    await this.storeVerificationCode(normalizedEmail, code)
    await this.redis.set(cooldownKey, '1', 'PX', cooldownMs)

    await this.audit.log({
      eventType: 'marketing.contact_verify_start',
      actor: normalizedEmail,
      metadata: { domain, mode: 'code-send' },
    })

    const ttlMinutes = waitlistConfig.verification.ttlMinutes || 30
    const subject = 'Verify your email for MYTE contact'
    const plain = `Here is your verification code: ${code}\n\nIt expires in ${ttlMinutes} minutes.`
    const html = `<p>Here is your verification code:</p><p style="font-size:20px;font-weight:700;letter-spacing:4px">${code}</p><p>It expires in ${ttlMinutes} minutes.</p>`

    await this.email.sendBrandedEmail({
      email: normalizedEmail,
      subject,
      title: `Verify your email, ${dto.name || 'there'}`,
      bodyText: plain,
      bodyHtml: html,
      footerNote: 'You requested this code to contact MYTE. If this was not you, you can ignore this email.',
    })

    return { status: 'sent' }
  }

  async confirmVerification(dto: ConfirmContactInquiryDto) {
    const normalizedEmail = this.normalizeEmail(dto.email)
    const code = await this.readVerificationCode(normalizedEmail)
    if (!code) {
      throw new BadRequestException('Verification code is invalid or expired. Request a new one.')
    }

    const trimmed = dto.code.trim()
    const attempt = await this.bumpVerifyAttempt(normalizedEmail)
    const maxAttempts = waitlistConfig.verification.maxAttempts || 5
    if (attempt > maxAttempts) {
      await this.redis.del(this.verifyCodeKey(normalizedEmail))
      throw new ForbiddenException('Too many attempts. Please request a new code.')
    }

    if (code !== trimmed) {
      throw new ForbiddenException('Invalid or expired code. Please request a new one.')
    }

    await this.markEmailVerified(normalizedEmail)
    await this.audit.log({
      eventType: 'marketing.contact_verify_success',
      actor: normalizedEmail,
      metadata: { mode: 'code-confirm' },
    })

    return { status: 'verified' }
  }

  private async maybeJoinWaitlist(
    dto: CreateContactInquiryDto,
    email: string,
    ip?: string
  ): Promise<{ status: string; entry?: any } | null> {
    if (!dto.joinWaitlist) return null
    const payload = {
      name: dto.name,
      email,
      role: dto.waitlistRole || 'Contact inquiry',
      source: dto.source || 'footer-contact',
      preCreateAccount: dto.preCreateAccount ?? false,
      marketingConsent: dto.marketingConsent ?? true,
      captchaToken: dto.captchaToken,
      trap: dto.trap,
    }
    try {
      const result = await this.waitlist.start(payload as any, ip)
      return result
    } catch (err: any) {
      this.logger.warn(`Failed to opt-in to waitlist from contact inquiry: ${err?.message || err}`)
      await this.audit.log({
        eventType: 'marketing.contact_waitlist_failed',
        actor: email,
        metadata: { error: err?.message },
      })
      return null
    }
  }

  async create(dto: CreateContactInquiryDto, ip?: string, userAgent?: string | null) {
    if (dto.trap) {
      return { status: 'ok' }
    }

    const normalizedEmail = this.normalizeEmail(dto.email)
    const personal = this.isPersonalEmail(normalizedEmail)

    await this.assertCaptcha(dto.captchaToken, ip)
    await this.assertContactAllowed(normalizedEmail, ip)
    const verified = await this.isEmailVerified(normalizedEmail)
    if (!verified) {
      throw new ForbiddenException('Email not verified yet. Please verify before sending.')
    }

    const entry = await this.model.create({
      name: dto.name,
      email: normalizedEmail,
      message: dto.message,
      source: dto.source || 'footer-contact',
      status: 'new',
      ip: ip || null,
      userAgent: userAgent || null,
    })

    const waitlistResult = !personal ? await this.maybeJoinWaitlist(dto, normalizedEmail, ip) : null

    await this.audit.log({
      eventType: 'marketing.contact_submit',
      actor: normalizedEmail,
      metadata: {
        id: entry.id,
        source: dto.source || 'footer-contact',
        joinWaitlist: !!dto.joinWaitlist && !personal,
        waitlistStatus: waitlistResult?.status,
      },
    })

    const subject = `New inquiry from ${dto.name} (${normalizedEmail})`
    const plain = `Name: ${dto.name}\nEmail: ${normalizedEmail}\nSource: ${
      dto.source || 'footer-contact'
    }\n\nMessage:\n${dto.message}`
    const html = `<p><strong>Name:</strong> ${dto.name}</p><p><strong>Email:</strong> ${normalizedEmail}</p><p><strong>Source:</strong> ${
      dto.source || 'footer-contact'
    }</p><p><strong>Message:</strong></p><p>${dto.message.replace(/\n/g, '<br/>')}</p>`

    await this.email.sendBrandedEmail({
      email: 'ahmed.mekallach@mytegroup.com',
      subject,
      title: 'New inquiry from the landing page',
      bodyText: plain,
      bodyHtml: html,
      footerNote: 'This inquiry was logged in the MYTE Construction OS event log for auditability.',
    })

    return entry.toObject ? entry.toObject() : entry
  }

  async list(params: { status?: string; page?: number; limit?: number }): Promise<ContactInquiryListResult> {
    const { status, page = 1, limit = 25 } = params || {}
    const query: any = {}
    if (status) query.status = status
    query.archivedAt = null
    const skip = (page - 1) * limit
    const [data, total] = await Promise.all([
      this.model.find(query).sort({ createdAt: -1 }).skip(skip).limit(limit).lean(),
      this.model.countDocuments(query),
    ])
    return { data: data as any[], total, page, limit }
  }

  async update(id: string, dto: UpdateContactInquiryDto, actorId?: string) {
    const update: any = {}
    if (dto.status) {
      update.status = dto.status
      if (dto.status === 'closed') {
        update.respondedAt = new Date()
        update.respondedBy = actorId || null
      }
    }
    const updated = await this.model.findOneAndUpdate({ _id: id }, { $set: update }, { new: true }).lean()
    if (!updated) {
      throw new ForbiddenException('Contact inquiry not found')
    }
    await this.audit.log({
      eventType: 'marketing.contact_update',
      actor: actorId || 'system',
      metadata: { id, status: updated.status },
    })
    return updated
  }
}
