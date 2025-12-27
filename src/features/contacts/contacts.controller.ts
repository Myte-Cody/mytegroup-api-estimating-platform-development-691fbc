import { BadRequestException, Body, Controller, Get, GoneException, Param, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
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

  private throwDeprecatedWrite() {
    throw new GoneException('Contacts write APIs are deprecated; use /persons, /companies, /company-locations');
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.SuperAdmin)
  @Get()
  async list(
    @Req() req: Request,
    @Query('orgId') orgId?: string,
    @Query('includeArchived') includeArchived?: string,
    @Query('personType') personType?: string
  ) {
    const actor = this.getActor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, orgId);
    if (!resolvedOrgId) throw new BadRequestException('Organization context is required');
    return this.contacts.list(actor, resolvedOrgId, this.parseIncludeArchived(includeArchived), personType);
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.SuperAdmin)
  @Get(':id')
  async getById(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('includeArchived') includeArchived?: string,
    @Query('orgId') orgId?: string,
  ) {
    const actor = this.getActor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, orgId);
    if (!resolvedOrgId && actor.role !== Role.SuperAdmin) throw new BadRequestException('Organization context is required');
    return this.contacts.getById(actor, id, resolvedOrgId, this.parseIncludeArchived(includeArchived));
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.SuperAdmin)
  @Post()
  async create(
    @Body() dto: CreateContactDto,
    @Req() req: Request,
    @Query('orgId') orgId?: string,
  ) {
    this.throwDeprecatedWrite();
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.SuperAdmin)
  @Patch(':id')
  async update(
    @Param('id') id: string,
    @Body() dto: UpdateContactDto,
    @Req() req: Request,
    @Query('orgId') orgId?: string,
  ) {
    this.throwDeprecatedWrite();
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.SuperAdmin)
  @Post(':id/archive')
  async archive(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('orgId') orgId?: string,
  ) {
    this.throwDeprecatedWrite();
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.SuperAdmin)
  @Post(':id/unarchive')
  async unarchive(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('orgId') orgId?: string,
  ) {
    this.throwDeprecatedWrite();
  }
}
