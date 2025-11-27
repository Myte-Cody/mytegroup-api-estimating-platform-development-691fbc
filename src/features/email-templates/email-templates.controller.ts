import {
  BadRequestException,
  Body,
  Controller,
  ForbiddenException,
  Get,
  Param,
  Post,
  Put,
  Query,
  Req,
  UseGuards,
} from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { emailTestConfig } from '../../config/app.config';
import { EmailTemplatesService } from './email-templates.service';
import { UpdateEmailTemplateDto } from './dto/update-email-template.dto';
import { PreviewEmailTemplateDto } from './dto/preview-email-template.dto';
import { TestSendTemplateDto } from './dto/test-send-template.dto';
import { EMAIL_TEMPLATE_DEFAULT_LOCALE, EmailTemplateName } from './email-template.constants';
import { EmailService } from '../email/email.service';
import { AuditLogService } from '../../common/services/audit-log.service';

@Controller('email-templates')
export class EmailTemplatesController {
  constructor(
    private readonly templates: EmailTemplatesService,
    private readonly email: EmailService,
    private readonly audit: AuditLogService
  ) {}

  private resolveOrgId(req: Request) {
    const user = req.session?.user;
    const requested = (req.query.orgId as string) || user?.orgId;
    if (!requested) {
      throw new BadRequestException('orgId is required for template operations');
    }
    if (user?.role !== Role.SuperAdmin && user?.orgId && requested !== user.orgId) {
      throw new ForbiddenException('Cannot manage templates for another organization');
    }
    return requested;
  }

  private assertAllowedRecipient(email: string) {
    const allowed = (emailTestConfig.allowedRecipients || []).map((v) => v.toLowerCase());
    if (!allowed.length) return true;
    const normalized = email.toLowerCase();
    if (!allowed.includes(normalized)) {
      throw new ForbiddenException('Recipient not allowed for test sends');
    }
    return true;
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.Compliance)
  @Get()
  list(@Req() req: Request, @Query('locale') locale?: string) {
    const orgId = this.resolveOrgId(req);
    return this.templates.list(orgId, locale || EMAIL_TEMPLATE_DEFAULT_LOCALE);
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.Compliance)
  @Get(':name')
  getOne(@Param('name') name: EmailTemplateName, @Req() req: Request, @Query('locale') locale?: string) {
    const orgId = this.resolveOrgId(req);
    return this.templates.get(orgId, name, locale || EMAIL_TEMPLATE_DEFAULT_LOCALE);
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin)
  @Put(':name')
  update(
    @Param('name') name: EmailTemplateName,
    @Body() dto: UpdateEmailTemplateDto,
    @Req() req: Request
  ) {
    const userId = req.session?.user?.id;
    const orgId = this.resolveOrgId(req);
    return this.templates.update(orgId, userId, name, dto);
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.Compliance)
  @Post(':name/preview')
  async preview(
    @Param('name') name: EmailTemplateName,
    @Body() dto: PreviewEmailTemplateDto,
    @Req() req: Request
  ) {
    const userId = req.session?.user?.id;
    const orgId = this.resolveOrgId(req);
    const locale = dto.locale || EMAIL_TEMPLATE_DEFAULT_LOCALE;
    const result = await this.templates.render(orgId, name, dto.variables || {}, locale);
    await this.audit.log({
      eventType: 'email_template.preview',
      orgId,
      userId,
      entity: 'EmailTemplate',
      metadata: { name, locale },
    });
    return result;
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin)
  @Post(':name/test-send')
  async testSend(
    @Param('name') name: EmailTemplateName,
    @Body() dto: TestSendTemplateDto,
    @Req() req: Request
  ) {
    const userId = req.session?.user?.id;
    const orgId = this.resolveOrgId(req);
    const locale = dto.locale || EMAIL_TEMPLATE_DEFAULT_LOCALE;
    this.assertAllowedRecipient(dto.to);
    const { rendered } = await this.templates.render(orgId, name, dto.variables || {}, locale);
    await this.audit.log({
      eventType: 'email_template.test_send',
      orgId,
      userId,
      entity: 'EmailTemplate',
      metadata: { name, locale, to: dto.to, mode: 'test' },
    });
    await this.email.sendMail({
      email: dto.to,
      subject: rendered.subject,
      text: rendered.text,
      html: rendered.html,
      templateName: name,
      orgId,
      variables: dto.variables,
      mode: 'test',
    });
    return { status: 'queued', to: dto.to };
  }
}
