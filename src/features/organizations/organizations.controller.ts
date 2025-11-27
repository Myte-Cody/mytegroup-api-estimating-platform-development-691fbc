import { Controller, Post, Body, UseGuards, Patch, Param, Req, ForbiddenException, Get } from '@nestjs/common';
import { OrganizationsService } from './organizations.service';
import { CreateOrganizationDto } from './dto/create-organization.dto';
import { SessionGuard } from '../../common/guards/session.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role, expandRoles, mergeRoles } from '../../common/roles';
import { UpdateOrganizationDatastoreDto } from './dto/update-organization-datastore.dto';
import { Request } from 'express';
import { UpdateOrganizationDto } from './dto/update-organization.dto';
import { UpdateOrganizationLegalHoldDto } from './dto/update-organization-legal-hold.dto';
import { UpdateOrganizationPiiDto } from './dto/update-organization-pii.dto';
@Controller('organizations')
export class OrganizationsController {
  constructor(private svc: OrganizationsService) {}

  private assertOrgAccess(actor: any, orgId: string) {
    if (!actor) throw new ForbiddenException('Missing session');
    const roles = mergeRoles(actor.role as Role, actor.roles as Role[]);
    const effective = expandRoles(roles);
    if (effective.includes(Role.SuperAdmin) || effective.includes(Role.PlatformAdmin)) return;
    if (actor.orgId !== orgId) {
      throw new ForbiddenException('Cannot operate on another organization');
    }
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Post()
  // POST /organizations - SuperAdmin only; creates org with datastore/compliance defaults.
  create(@Body() dto: CreateOrganizationDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.create(dto, actor);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin, Role.OrgOwner, Role.Admin, Role.PlatformAdmin)
  @Get(':id')
  // GET /organizations/:id - SuperAdmin or same-org Admin/Owner.
  async findOne(@Param('id') id: string, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    this.assertOrgAccess(actor, id);
    return this.svc.findById(id);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin, Role.OrgOwner, Role.Admin)
  @Patch(':id')
  // PATCH /organizations/:id - SuperAdmin or same-org Admin/Owner; updates name/metadata only.
  update(@Param('id') id: string, @Body() dto: UpdateOrganizationDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    this.assertOrgAccess(actor, id);
    return this.svc.update(id, dto, actor);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Patch(':id/archive')
  // PATCH /organizations/:id/archive - SuperAdmin only; legalHold blocks.
  archive(@Param('id') id: string, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.archive(id, actor);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Patch(':id/unarchive')
  // PATCH /organizations/:id/unarchive - SuperAdmin only.
  unarchive(@Param('id') id: string, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.unarchive(id, actor);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Patch(':id/datastore')
  // PATCH /organizations/:id/datastore - SuperAdmin only; switches shared<->dedicated with validation/audit.
  updateDatastore(@Param('id') id: string, @Body() dto: UpdateOrganizationDatastoreDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.updateDatastore(id, dto, actor);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Post(':id/legal-hold')
  // POST /organizations/:id/legal-hold - SuperAdmin only; sets legalHold flag.
  setLegalHold(@Param('id') id: string, @Body() dto: UpdateOrganizationLegalHoldDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.setLegalHold(id, dto, actor);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Post(':id/pii-stripped')
  // POST /organizations/:id/pii-stripped - SuperAdmin only; toggles piiStripped flag.
  setPiiStripped(@Param('id') id: string, @Body() dto: UpdateOrganizationPiiDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.setPiiStripped(id, dto, actor);
  }
}
