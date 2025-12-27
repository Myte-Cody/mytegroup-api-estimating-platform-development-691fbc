import { Body, Controller, Get, Param, Patch, Post, Query, Req, UseGuards, ForbiddenException } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { CreatePersonDto } from './dto/create-person.dto';
import { ListPersonsQueryDto } from './dto/list-persons.dto';
import { UpdatePersonDto } from './dto/update-person.dto';
import { PersonsService } from './persons.service';

@Controller('persons')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class PersonsController {
  constructor(private readonly svc: PersonsService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  private resolveOrgId(actorOrgId?: string, actorRole?: Role, requestedOrgId?: string) {
    const privileged = actorRole === Role.SuperAdmin || actorRole === Role.PlatformAdmin;
    const orgId = privileged && requestedOrgId ? requestedOrgId : actorOrgId;
    if (!orgId) throw new ForbiddenException('Missing organization context');
    return orgId;
  }

  @Roles(Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin)
  @Get()
  list(
    @Query() query: ListPersonsQueryDto,
    @Req() req: Request,
  ) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, query.orgId);
    return this.svc.list(actor, resolvedOrgId, query);
  }

  @Roles(Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin)
  @Post()
  create(
    @Body() dto: CreatePersonDto,
    @Req() req: Request,
    @Query('orgId') orgId?: string
  ) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, orgId);
    return this.svc.create(actor, resolvedOrgId, dto);
  }

  @Roles(Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin)
  @Get(':id')
  getById(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('includeArchived') includeArchived?: string,
    @Query('orgId') orgId?: string
  ) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, orgId);
    const include = includeArchived === 'true' || includeArchived === '1';
    return this.svc.getById(actor, resolvedOrgId, id, include);
  }

  @Roles(Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin)
  @Patch(':id')
  update(
    @Param('id') id: string,
    @Body() dto: UpdatePersonDto,
    @Req() req: Request,
    @Query('orgId') orgId?: string
  ) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, orgId);
    return this.svc.update(actor, resolvedOrgId, id, dto);
  }

  @Roles(Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin)
  @Post(':id/archive')
  archive(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('orgId') orgId?: string
  ) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, orgId);
    return this.svc.archive(actor, resolvedOrgId, id);
  }

  @Roles(Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin)
  @Post(':id/unarchive')
  unarchive(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('orgId') orgId?: string
  ) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, orgId);
    return this.svc.unarchive(actor, resolvedOrgId, id);
  }
}
