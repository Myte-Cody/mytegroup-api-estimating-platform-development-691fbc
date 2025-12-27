import { Body, Controller, Delete, Get, Param, ParseEnumPipe, Patch, Query, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { UpdateUserRolesDto } from '../users/dto/update-roles.dto';
import { RbacService } from './rbac.service';

@Controller('rbac')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
@Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.OrgOwner, Role.OrgAdmin, Role.Admin)
export class RbacController {
  constructor(private readonly svc: RbacService) {}

  @Get('roles')
  hierarchy() {
    return this.svc.hierarchy();
  }

  @Get('users')
  list(@Req() req: Request, @Query('orgId') orgId?: string) {
    const actor = (req as any).session?.user || (req as any).user;
    return this.svc.listUserRoles(orgId, actor);
  }

  @Get('users/:id/roles')
  getUserRoles(@Param('id') id: string, @Req() req: Request) {
    const actor = (req as any).session?.user || (req as any).user;
    return this.svc.getUserRoles(id, actor);
  }

  @Patch('users/:id/roles')
  updateRoles(@Param('id') id: string, @Body() dto: UpdateUserRolesDto, @Req() req: Request) {
    const actor = (req as any).session?.user || (req as any).user;
    return this.svc.updateUserRoles(id, dto.roles, actor);
  }

  @Delete('users/:id/roles/:role')
  revokeRole(
    @Param('id') id: string,
    @Param('role', new ParseEnumPipe(Role)) role: Role,
    @Req() req: Request
  ) {
    const actor = (req as any).session?.user || (req as any).user;
    return this.svc.revokeRole(id, role, actor);
  }
}
