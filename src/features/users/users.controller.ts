import { Controller, Post, Body, Req, UseGuards, Patch, Param, ForbiddenException, Get, Query } from '@nestjs/common';
import { Request } from 'express';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { SessionGuard } from '../../common/guards/session.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { RolesGuard } from '../../common/guards/roles.guard';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { Role } from '../../common/roles';
import { UpdateUserRolesDto } from './dto/update-roles.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { GetUserQueryDto, ListUsersQueryDto } from './dto/list-users.dto';

@Controller('users')
export class UsersController {
  constructor(private svc: UsersService) {}

  // Roles: admin, org owner, platform admin, superadmin
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin, Role.OrgOwner, Role.PlatformAdmin, Role.OrgAdmin)
  @Post()
  create(@Body() dto: CreateUserDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    const actorRoles: Role[] = (actor?.roles as Role[]) || [];
    const canBypassOrg =
      actorRoles.includes(Role.SuperAdmin) ||
      actorRoles.includes(Role.PlatformAdmin) ||
      actor?.role === Role.SuperAdmin ||
      actor?.role === Role.PlatformAdmin;
    const organizationId = actor?.orgId || (canBypassOrg ? dto.organizationId : undefined);
    if (!organizationId) throw new ForbiddenException('Missing organization context');
    return this.svc.create({ ...dto, organizationId }, actor, { enforceSeat: true });
  }

  // Roles: admin, org admin, org owner, platform admin, superadmin
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgAdmin, Role.SuperAdmin, Role.OrgOwner, Role.PlatformAdmin)
  @Get()
  list(@Query() query: ListUsersQueryDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.list(actor, {
      orgId: query.organizationId,
      includeArchived: query.includeArchived,
    });
  }

  // Roles: admin, org admin, org owner, platform admin, superadmin
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgAdmin, Role.SuperAdmin, Role.OrgOwner, Role.PlatformAdmin)
  @Get(':id')
  getById(@Param('id') id: string, @Query() query: GetUserQueryDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.getById(id, actor, query.includeArchived);
  }

  // Roles: admin, org admin, org owner, platform admin, superadmin
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgAdmin, Role.SuperAdmin, Role.OrgOwner, Role.PlatformAdmin)
  @Patch(':id')
  update(@Param('id') id: string, @Body() dto: UpdateUserDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.update(id, dto, actor);
  }

  // Roles: admin, org owner, platform admin, superadmin
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgAdmin, Role.SuperAdmin, Role.OrgOwner, Role.PlatformAdmin)
  @Patch(':id/archive')
  archive(@Param('id') id: string, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.archiveUser(id, {
      orgId: actor?.orgId,
      role: actor?.role,
      roles: (actor?.roles as Role[]) || [],
      id: actor?.id,
    });
  }

  // Roles: admin, org owner, platform admin, superadmin
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgAdmin, Role.SuperAdmin, Role.OrgOwner, Role.PlatformAdmin)
  @Patch(':id/unarchive')
  unarchive(@Param('id') id: string, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.unarchiveUser(id, {
      orgId: actor?.orgId,
      role: actor?.role,
      roles: (actor?.roles as Role[]) || [],
      id: actor?.id,
    });
  }

  // Roles: admin, org owner, platform admin, org admin, superadmin
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin, Role.OrgOwner, Role.PlatformAdmin, Role.OrgAdmin)
  @Patch(':id/roles')
  updateRoles(@Param('id') id: string, @Body() dto: UpdateUserRolesDto, @Req() req: Request) {
    const actor = (req as any).user || req.session?.user;
    return this.svc.updateRoles(id, dto.roles, {
      orgId: actor?.orgId,
      role: actor?.role,
      roles: (actor?.roles as Role[]) || [],
      id: actor?.id,
    });
  }
}
