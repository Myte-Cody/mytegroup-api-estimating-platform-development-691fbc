import { Controller, Post, Body, Req, UseGuards, Patch, Param } from '@nestjs/common';
import { Request } from 'express';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { SessionGuard } from '../../common/guards/session.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { RolesGuard } from '../../common/guards/roles.guard';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { Role } from '../../common/roles';
import { ForbiddenException } from '@nestjs/common';
@Controller('users')
export class UsersController {
  constructor(private svc: UsersService) {}
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin, Role.OrgOwner)
  @Post()
  create(@Body() dto: CreateUserDto, @Req() req: Request) {
    const orgId = req.session?.user && req.session.user.orgId;
    if (!orgId) throw new ForbiddenException('Missing organization context');
    return this.svc.create({ ...dto, organizationId: orgId });
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin, Role.OrgOwner)
  @Patch(':id/archive')
  archive(@Param('id') id: string, @Req() req: Request) {
    const actor = req.session?.user;
    if (!actor?.orgId && actor?.role !== Role.SuperAdmin) {
      throw new ForbiddenException('Missing organization context');
    }
    return this.svc.archiveUser(id, { orgId: actor.orgId, role: actor.role });
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin, Role.OrgOwner)
  @Patch(':id/unarchive')
  unarchive(@Param('id') id: string, @Req() req: Request) {
    const actor = req.session?.user;
    if (!actor?.orgId && actor?.role !== Role.SuperAdmin) {
      throw new ForbiddenException('Missing organization context');
    }
    return this.svc.unarchiveUser(id, { orgId: actor.orgId, role: actor.role });
  }
}
