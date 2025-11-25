import { Body, Controller, Get, Param, Patch, Post, Query, Req, UseGuards, ForbiddenException } from '@nestjs/common';
import { Request } from 'express';
import { SessionGuard } from '../../common/guards/session.guard';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../common/roles';
import { OfficesService } from './offices.service';
import { CreateOfficeDto } from './dto/create-office.dto';

@Controller('offices')
export class OfficesController {
  constructor(private svc: OfficesService) {}

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin)
  @Post()
  create(@Body() dto: CreateOfficeDto, @Req() req: Request) {
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
