import { BadRequestException, Injectable } from '@nestjs/common';
import { AuditLogService } from '../../common/services/audit-log.service';
import { SendEmailDto } from './dto/send-email.dto';
import * as nodemailer from 'nodemailer';
import { buildClientUrl, smtpConfig } from '../../config/app.config';
import { EmailTemplatesService } from '../email-templates/email-templates.service';
import { EmailTemplateName } from '../email-templates/email-template.constants';
import {
  inviteTemplate,
  passwordResetTemplate,
  verificationTemplate,
  renderBrandedTemplate,
  waitlistVerificationTemplate,
  waitlistInviteTemplate,
} from './templates';

@Injectable()
export class EmailService {
  private readonly mailConfig = smtpConfig();
  private readonly transporter = this.createTransporter();
  public readonly sentMessages: Array<{ to: string; subject: string }> = [];

  constructor(private readonly audit: AuditLogService, private readonly templates: EmailTemplatesService) {}

  private createTransporter() {
    if (this.mailConfig.stubTransport) {
      return nodemailer.createTransport({
        streamTransport: true,
        newline: 'unix',
        buffer: true,
      } as any);
    }
    return nodemailer.createTransport({
      host: this.mailConfig.host,
      port: this.mailConfig.port,
      secure: this.mailConfig.secure,
      auth: this.mailConfig.auth,
    });
  }

  private resolveText(dto: SendEmailDto) {
    const text = dto.text ?? dto.body;
    if (!text && !dto.html) {
      throw new BadRequestException('Email text or html body is required');
    }
    return text;
  }

  async sendBrandedEmail(opts: {
    email: string;
    subject: string;
    title: string;
    bodyHtml: string;
    bodyText: string;
    ctaLabel?: string;
    ctaHref?: string;
    footerNote?: string;
  }) {
    const html = renderBrandedTemplate({
      title: opts.title,
      bodyHtml: opts.bodyHtml,
      ctaLabel: opts.ctaLabel,
      ctaHref: opts.ctaHref,
      footerNote: opts.footerNote,
    });
    return this.sendMail({
      email: opts.email,
      subject: opts.subject,
      text: opts.bodyText,
      html,
    });
  }

  async sendMail(dto: SendEmailDto) {
    const cfg = this.mailConfig;
    const text = this.resolveText(dto);
    const from = cfg.from || cfg.auth?.user || 'no-reply@myte.test';
    const bcc = Array.isArray(dto.bcc) ? dto.bcc.filter(Boolean) : [];
    const metadata = {
      to: dto.email,
      subject: dto.subject,
      template: dto.templateName,
      orgId: dto.orgId,
      variableKeys: dto.variables ? Object.keys(dto.variables) : undefined,
      mode: dto.mode || 'live',
      bccCount: bcc.length || undefined,
    };
    await this.audit.log({
      eventType: 'email.send_attempt',
      metadata,
    });
    const send = async () =>
      this.transporter.sendMail({
        from,
        to: dto.email,
        subject: dto.subject,
        text,
        html: dto.html,
        bcc: bcc.length ? bcc : undefined,
      });
    try {
      const info = await send();
      if (cfg.stubTransport) {
        this.sentMessages.push({ to: dto.email, subject: dto.subject });
      }
      await this.audit.log({
        eventType: 'email.send',
        metadata: { ...metadata, status: 'sent' },
      });
      return { status: 'sent', to: dto.email, info };
    } catch (err) {
      // retry once
      try {
        const info = await send();
        if (cfg.stubTransport) {
          this.sentMessages.push({ to: dto.email, subject: dto.subject });
        }
        await this.audit.log({
          eventType: 'email.send',
          metadata: { ...metadata, status: 'sent-retry' },
        });
        return { status: 'sent-retry', to: dto.email, info };
      } catch (err2) {
        await this.audit.log({
          eventType: 'email.send_failed',
          metadata: { ...metadata, status: 'failed', error: (err2 as Error)?.message },
        });
        throw err2;
      }
    }
  }

  async sendBulkMail(opts: { bcc: string[]; subject: string; text?: string; html?: string; to?: string }) {
    const cfg = this.mailConfig;
    const to = opts.to || cfg.from || cfg.auth?.user || 'no-reply@myte.test';
    return this.sendMail({
      email: to,
      subject: opts.subject,
      text: opts.text,
      html: opts.html,
      bcc: opts.bcc,
    });
  }

  private async sendTemplateEmail(
    orgId: string,
    template: EmailTemplateName,
    email: string,
    variables: Record<string, unknown>
  ) {
    const { rendered } = await this.templates.render(orgId, template, variables);
    return this.sendMail({
      email,
      subject: rendered.subject,
      text: rendered.text,
      html: rendered.html,
      templateName: template,
      orgId,
      variables,
    });
  }

  async sendVerificationEmail(email: string, token: string, orgId: string, userName?: string) {
    const link = buildClientUrl(`/verify-email?token=${encodeURIComponent(token)}`);
    const t = verificationTemplate({ verifyLink: link, userName });
    await this.sendMail({ email, subject: t.subject, text: t.text, html: t.html });
  }

  async sendPasswordResetEmail(email: string, token: string, orgId: string, userName?: string) {
    const link = buildClientUrl(`/reset-password?token=${encodeURIComponent(token)}`);
    const t = passwordResetTemplate({ resetLink: link, userName });
    await this.sendMail({ email, subject: t.subject, text: t.text, html: t.html });
  }

  async sendInviteEmail(email: string, token: string, orgId: string, orgName?: string, userName?: string) {
    const link = buildClientUrl(`/invite/accept?token=${encodeURIComponent(token)}`);
    const t = inviteTemplate({ inviteLink: link, orgName, userName });
    await this.sendMail({ email, subject: t.subject, text: t.text, html: t.html });
  }

  async sendPreviewSet(to: string) {
    const host = process.env.ROOT_DOMAIN_PROD || process.env.ROOT_DOMAIN || 'localhost:4001';
    const registerLink = `https://${host}/auth/register`;
    const verifyLink = `https://${host}/verify-email?token=sample-token`;
    const resetLink = `https://${host}/reset-password?token=sample-token`;
    const inviteLink = `https://${host}/invite/accept?token=sample-token`;
    const previews = [
      waitlistVerificationTemplate({ code: '384219', userName: 'Crew Lead' }),
      waitlistInviteTemplate({ registerLink, domain: 'example.com', calendly: 'https://calendly.com/ahmed-mekallach/thought-exchange' }),
      verificationTemplate({ verifyLink, userName: 'Crew Lead' }),
      passwordResetTemplate({ resetLink, userName: 'Crew Lead' }),
      inviteTemplate({ inviteLink, orgName: 'Myte Demo Org', userName: 'Crew Lead' }),
    ];
    const results = [];
    for (const msg of previews) {
      const res = await this.sendMail({ email: to, subject: `${msg.subject} (preview)`, text: msg.text, html: msg.html });
      results.push(res);
    }
    return { sent: results.length };
  }
}
