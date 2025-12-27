import { Body, Controller, Get, Param, Patch, Post, Query, Req, UseGuards, ForbiddenException } from '@nestjs/common';
import { Request } from 'express';
import { SessionGuard } from '../../common/guards/session.guard';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../common/roles';
import { OfficesService } from './offices.service';
import { CreateOfficeDto } from './dto/create-office.dto';
import { ListOfficesQueryDto } from './dto/list-offices.dto';
import { UpdateOfficeDto } from './dto/update-office.dto';

@Controller(['offices', 'org-locations'])
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class OfficesController {
  constructor(private svc: OfficesService) {}

  private getActor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  private parseIncludeArchived(raw?: string) {
    return raw === 'true' || raw === '1';
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.SuperAdmin)
  @Post()
  create(@Body() dto: CreateOfficeDto, @Req() req: Request) {
    const actor = this.getActor(req);
    return this.svc.create(dto, actor);
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.Viewer, Role.SuperAdmin)
  @Get()
  list(
    @Req() req: Request,
    @Query() query: ListOfficesQueryDto
  ) {
    const actor = this.getActor(req);
    const resolvedOrg = actor.role === Role.SuperAdmin && query.orgId ? query.orgId : actor.orgId;
    if (!resolvedOrg) throw new ForbiddenException('Missing organization context');
    return this.svc.list(actor, resolvedOrg, query);
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.PM, Role.Viewer, Role.SuperAdmin)
  @Get(':id')
  getById(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('includeArchived') includeArchived?: string,
    @Query('orgId') orgId?: string,
  ) {
    const actor = this.getActor(req);
    return this.svc.getById(id, actor, orgId, this.parseIncludeArchived(includeArchived));
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.SuperAdmin)
  @Patch(':id')
  update(
    @Param('id') id: string,
    @Body() dto: UpdateOfficeDto,
    @Req() req: Request,
    @Query('orgId') orgId?: string,
  ) {
    const actor = this.getActor(req);
    return this.svc.update(id, dto, actor, orgId);
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.SuperAdmin)
  @Post(':id/archive')
  archive(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('orgId') orgId?: string,
  ) {
    const actor = this.getActor(req);
    return this.svc.archive(id, actor, orgId);
  }

  @Roles(Role.Admin, Role.Manager, Role.OrgOwner, Role.SuperAdmin)
  @Post(':id/unarchive')
  unarchive(
    @Param('id') id: string,
    @Req() req: Request,
    @Query('orgId') orgId?: string,
  ) {
    const actor = this.getActor(req);
    return this.svc.unarchive(id, actor, orgId);
  }
}
