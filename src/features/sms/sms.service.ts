import { Injectable, Logger } from '@nestjs/common'

type TwilioConfig = {
  accountSid?: string
  authToken?: string
  from?: string
  stub: boolean
}

const twilioConfig = (): TwilioConfig => {
  const accountSid = (process.env.TWILIO_ACCOUNT_SID || '').trim()
  const authToken = (process.env.TWILIO_AUTH_TOKEN || '').trim()
  const from = (process.env.TWILIO_PHONE_NUMBER || '').trim().replace(/^['"]|['"]$/g, '')
  const stub = process.env.NODE_ENV === 'test' || !accountSid || !authToken || !from
  return { accountSid, authToken, from, stub }
}

@Injectable()
export class SmsService {
  private readonly logger = new Logger(SmsService.name)
  private readonly cfg = twilioConfig()

  async sendSms(to: string, body: string) {
    if (this.cfg.stub) {
      this.logger.debug(`[sms] stubbed send to=${to}`)
      return { status: 'stubbed' }
    }

    const url = `https://api.twilio.com/2010-04-01/Accounts/${this.cfg.accountSid}/Messages.json`
    const auth = Buffer.from(`${this.cfg.accountSid}:${this.cfg.authToken}`).toString('base64')
    const payload = new URLSearchParams()
    payload.set('From', this.cfg.from as string)
    payload.set('To', to)
    payload.set('Body', body)

    const res = await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${auth}`,
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: payload.toString(),
    })

    if (!res.ok) {
      const text = await res.text().catch(() => '')
      throw new Error(`Twilio send failed (${res.status}): ${text || res.statusText}`)
    }

    return { status: 'sent' }
  }
}

