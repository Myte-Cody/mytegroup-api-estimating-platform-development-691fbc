import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import {
  BUILTIN_EMAIL_TEMPLATES,
  EMAIL_TEMPLATE_DEFAULT_LOCALE,
  EmailTemplateDefinition,
  EmailTemplateName,
  EMAIL_TEMPLATE_VARIABLES,
} from './email-template.constants';
import { EmailTemplate, EmailTemplateSchema } from './schemas/email-template.schema';
import { UpdateEmailTemplateDto } from './dto/update-email-template.dto';
import { EmailTemplateRenderer, RenderedEmailTemplate } from './email-template.renderer';

export interface EmailTemplateView {
  id?: string;
  name: EmailTemplateName;
  locale: string;
  subject: string;
  html: string;
  text: string;
  requiredVariables: string[];
  optionalVariables: string[];
  source: 'custom' | 'default';
  archivedAt?: Date | null;
  legalHold?: boolean;
  updatedByUserId?: string | null;
  updatedAt?: Date | string;
}

@Injectable()
export class EmailTemplatesService {
  private readonly renderer = new EmailTemplateRenderer();

  constructor(
    @InjectModel('EmailTemplate') private readonly templateModel: Model<EmailTemplate>,
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService
  ) {}

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<EmailTemplate>(orgId, 'EmailTemplate', EmailTemplateSchema, this.templateModel);
  }

  private baseTemplate(name: EmailTemplateName, locale: string): EmailTemplateDefinition | undefined {
    const normalized = locale || EMAIL_TEMPLATE_DEFAULT_LOCALE;
    const direct = BUILTIN_EMAIL_TEMPLATES.find(
      (tpl) => tpl.name === name && (tpl.locale || EMAIL_TEMPLATE_DEFAULT_LOCALE) === normalized
    );
    if (direct) return direct;
    return BUILTIN_EMAIL_TEMPLATES.find(
      (tpl) => tpl.name === name && (tpl.locale || EMAIL_TEMPLATE_DEFAULT_LOCALE) === EMAIL_TEMPLATE_DEFAULT_LOCALE
    );
  }

  private toView(template: EmailTemplate | EmailTemplateDefinition, source: 'custom' | 'default'): EmailTemplateView {
    return {
      id: (template as any).id || undefined,
      name: template.name as EmailTemplateName,
      locale: (template as any).locale || EMAIL_TEMPLATE_DEFAULT_LOCALE,
      subject: template.subject,
      html: template.html,
      text: template.text,
      requiredVariables: template.requiredVariables || EMAIL_TEMPLATE_VARIABLES[template.name as EmailTemplateName],
      optionalVariables: (template as any).optionalVariables || [],
      source,
      archivedAt: (template as any).archivedAt || null,
      legalHold: (template as any).legalHold || false,
      updatedByUserId: (template as any).updatedByUserId || null,
      updatedAt: (template as any).updatedAt || undefined,
    };
  }

  private assertTemplateAllowed(name: EmailTemplateName) {
    const allowed = Object.keys(EMAIL_TEMPLATE_VARIABLES) as EmailTemplateName[];
    if (!allowed.includes(name)) {
      throw new NotFoundException('Template not recognized');
    }
  }

  async list(orgId: string, locale = EMAIL_TEMPLATE_DEFAULT_LOCALE): Promise<EmailTemplateView[]> {
    const model = await this.model(orgId);
    const custom = await model.find({ orgId, locale, archivedAt: null }).lean();
    const customMap = new Map<string, EmailTemplateView>();
    custom.forEach((tpl) => {
      customMap.set(`${tpl.name}:${tpl.locale}`, this.toView(tpl as any, 'custom'));
    });

    const results: EmailTemplateView[] = [];
    customMap.forEach((value) => results.push(value));

    BUILTIN_EMAIL_TEMPLATES.filter((tpl) => tpl.locale === locale || !tpl.locale).forEach((tpl) => {
      const key = `${tpl.name}:${tpl.locale || EMAIL_TEMPLATE_DEFAULT_LOCALE}`;
      if (!customMap.has(key)) {
        results.push(this.toView(tpl, 'default'));
      }
    });
    return results;
  }

  async get(orgId: string, name: EmailTemplateName, locale = EMAIL_TEMPLATE_DEFAULT_LOCALE): Promise<EmailTemplateView> {
    this.assertTemplateAllowed(name);
    const model = await this.model(orgId);
    const found = await model.findOne({ orgId, name, locale, archivedAt: null }).lean();
    if (found) {
      return this.toView(found as any, 'custom');
    }
    const fallback = this.baseTemplate(name, locale);
    if (!fallback) throw new NotFoundException('Template not found');
    return this.toView(fallback, 'default');
  }

  private ensurePlaceholders(dto: UpdateEmailTemplateDto, required: string[], name: EmailTemplateName) {
    this.renderer.ensurePlaceholdersPresent({
      name,
      html: dto.html,
      text: dto.text,
      requiredVariables: required,
    });
  }

  async update(
    orgId: string,
    userId: string,
    name: EmailTemplateName,
    dto: UpdateEmailTemplateDto
  ): Promise<EmailTemplateView> {
    this.assertTemplateAllowed(name);
    const required = EMAIL_TEMPLATE_VARIABLES[name];
    this.ensurePlaceholders(dto, required, name);
    const locale = dto.locale || EMAIL_TEMPLATE_DEFAULT_LOCALE;
    const model = await this.model(orgId);
    const existing = await model.findOne({ orgId, name, locale });
    if (existing && existing.legalHold) {
      throw new ForbiddenException('Template is under legal hold');
    }
    const payload = {
      orgId,
      name,
      locale,
      subject: dto.subject,
      html: dto.html,
      text: dto.text,
      requiredVariables: required,
      optionalVariables: this.baseTemplate(name, locale)?.optionalVariables || [],
      archivedAt: null,
      updatedByUserId: userId,
      createdByUserId: existing?.createdByUserId || userId,
    };
    const saved = existing ? Object.assign(existing, payload) : new model(payload);
    await saved.save();
    await this.audit.log({
      eventType: 'email_template.updated',
      orgId,
      userId,
      entity: 'EmailTemplate',
      entityId: (saved as any).id,
      metadata: { name, locale, source: existing ? 'custom' : 'created' },
    });
    return this.toView(saved as any, 'custom');
  }

  async render(
    orgId: string,
    name: EmailTemplateName,
    variables: Record<string, unknown>,
    locale = EMAIL_TEMPLATE_DEFAULT_LOCALE
  ): Promise<{ template: EmailTemplateView; rendered: RenderedEmailTemplate }> {
    const template = await this.get(orgId, name, locale);
    const rendered = this.renderer.render(template, variables);
    return { template, rendered };
  }
}
