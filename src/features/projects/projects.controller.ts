import { Body, Controller, Get, Param, Patch, Post, Query, Req, UseGuards, ForbiddenException } from '@nestjs/common';
import { Request } from 'express';
import { SessionGuard } from '../../common/guards/session.guard';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../common/roles';
import { ProjectsService } from './projects.service';
import { CreateProjectDto } from './dto/create-project.dto';

@Controller('projects')
export class ProjectsController {
  constructor(private svc: ProjectsService) {}

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin)
  @Post()
  create(@Body() dto: CreateProjectDto, @Req() req: Request) {
    const actor = req.session?.user;
    return this.svc.create(dto, { orgId: actor?.orgId, role: actor?.role });
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin)
  @Get()
  list(@Req() req: Request, @Query('organizationId') orgId?: string) {
    const actor = req.session?.user;
    const resolvedOrg = actor?.role === Role.SuperAdmin && orgId ? orgId : actor?.orgId;
    if (!resolvedOrg) throw new ForbiddenException('Missing organization context');
    return this.svc.list(resolvedOrg);
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin)
  @Patch(':id/archive')
  archive(@Param('id') id: string, @Req() req: Request) {
    const actor = req.session?.user;
    return this.svc.archive(id, { orgId: actor?.orgId, role: actor?.role });
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin)
  @Patch(':id/unarchive')
  unarchive(@Param('id') id: string, @Req() req: Request) {
    const actor = req.session?.user;
    return this.svc.unarchive(id, { orgId: actor?.orgId, role: actor?.role });
  }
}
