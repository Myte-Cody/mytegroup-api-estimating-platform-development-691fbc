import { Injectable } from '@nestjs/common';
import { AuditLogService } from '../../common/services/audit-log.service';
import { SendEmailDto } from './dto/send-email.dto';
import * as nodemailer from 'nodemailer';
import { buildClientUrl, smtpConfig } from '../../config/app.config';

@Injectable()
export class EmailService {
  private readonly transporter = nodemailer.createTransport(() => {
    const cfg = smtpConfig();
    return {
      host: cfg.host,
      port: cfg.port,
      secure: cfg.secure,
      auth: cfg.auth,
    };
  });

  constructor(private readonly audit: AuditLogService) {}

  async sendMail(dto: SendEmailDto) {
    const cfg = smtpConfig();
    await this.audit.log({
      eventType: 'email.send',
      metadata: { to: dto.email, subject: dto.subject },
    });
    const send = async () =>
      this.transporter.sendMail({
        from: cfg.from || cfg.auth?.user,
        to: dto.email,
        subject: dto.subject,
        text: dto.body,
      });
    try {
      await send();
      return { status: 'sent', to: dto.email };
    } catch (err) {
      // retry once
      try {
        await send();
        return { status: 'sent-retry', to: dto.email };
      } catch (err2) {
        await this.audit.log({
          eventType: 'email.send_failed',
          metadata: { to: dto.email, subject: dto.subject, error: (err2 as Error)?.message },
        });
        throw err2;
      }
    }
  }

  async sendVerificationEmail(email: string, token: string) {
    const link = buildClientUrl(`/verify-email?token=${encodeURIComponent(token)}`);
    await this.sendMail({
      email,
      subject: 'Verify your email',
      body: `Welcome to Myte Construction!\n\nPlease verify your email by clicking the link below:\n${link}\n\nIf you did not sign up, please ignore this email.`,
    });
  }

  async sendPasswordResetEmail(email: string, token: string) {
    const link = buildClientUrl(`/reset-password?token=${encodeURIComponent(token)}`);
    await this.sendMail({
      email,
      subject: 'Reset your password',
      body: `We received a request to reset your password.\n\nReset link:\n${link}\n\nIf you did not request this, you can ignore this email.`,
    });
  }

  async sendInviteEmail(email: string, token: string) {
    const link = buildClientUrl(`/invite/accept?token=${encodeURIComponent(token)}`);
    await this.sendMail({
      email,
      subject: 'You have been invited to Myte Construction',
      body: `You have been invited to join an organization in Myte Construction.\n\nAccept your invite:\n${link}\n\nIf you did not expect this, you can ignore this email.`,
    });
  }
}
