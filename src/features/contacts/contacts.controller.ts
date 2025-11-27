import { BadRequestException, Body, Controller, Get, Param, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { CreateContactDto } from './dto/create-contact.dto';
import { UpdateContactDto } from './dto/update-contact.dto';
import { ContactsService } from './contacts.service';

@Controller('contacts')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class ContactsController {
  constructor(private readonly contacts: ContactsService) {}

  private getActor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  private parseIncludeArchived(raw?: string) {
    return raw === 'true' || raw === '1';
  }

  private resolveOrgId(actorOrgId?: string, requestedOrgId?: string) {
    return requestedOrgId || actorOrgId;
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.SuperAdmin)
  @Get()
  async list(
    @Req() req: Request,
    @Query('organizationId') organizationId?: string,
    @Query('includeArchived') includeArchived?: string
  ) {
    const actor = this.getActor(req);
    const orgId = this.resolveOrgId(actor.orgId, organizationId);
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.list(actor, orgId, this.parseIncludeArchived(includeArchived));
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.SuperAdmin)
  @Get(':id')
  async getById(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('includeArchived') includeArchived?: string,
    @Query('organizationId') organizationId?: string
  ) {
    const actor = this.getActor(req);
    const orgId = this.resolveOrgId(actor.orgId, organizationId);
    if (!orgId && actor.role !== Role.SuperAdmin) throw new BadRequestException('Organization context is required');
    return this.contacts.getById(actor, id, orgId, this.parseIncludeArchived(includeArchived));
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.SuperAdmin)
  @Post()
  async create(@Body() dto: CreateContactDto, @Req() req: Request, @Query('organizationId') organizationId?: string) {
    const actor = this.getActor(req);
    const orgId = this.resolveOrgId(actor.orgId, organizationId);
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.create(actor, orgId, dto);
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.SuperAdmin)
  @Patch(':id')
  async update(
    @Param('id') id: string,
    @Body() dto: UpdateContactDto,
    @Req() req: Request,
    @Query('organizationId') organizationId?: string
  ) {
    const actor = this.getActor(req);
    const orgId = this.resolveOrgId(actor.orgId, organizationId);
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.update(actor, orgId, id, dto);
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.SuperAdmin)
  @Post(':id/archive')
  async archive(@Param('id') id: string, @Req() req: Request, @Query('organizationId') organizationId?: string) {
    const actor = this.getActor(req);
    const orgId = this.resolveOrgId(actor.orgId, organizationId);
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.archive(actor, orgId, id);
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.SuperAdmin)
  @Post(':id/unarchive')
  async unarchive(@Param('id') id: string, @Req() req: Request, @Query('organizationId') organizationId?: string) {
    const actor = this.getActor(req);
    const orgId = this.resolveOrgId(actor.orgId, organizationId);
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.unarchive(actor, orgId, id);
  }
}
