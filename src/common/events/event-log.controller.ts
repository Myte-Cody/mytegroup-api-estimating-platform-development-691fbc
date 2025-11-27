import { Controller, ForbiddenException, Get, Param, Query, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../decorators/roles.decorator';
import { OrgScopeGuard } from '../guards/org-scope.guard';
import { RolesGuard } from '../guards/roles.guard';
import { SessionGuard } from '../guards/session.guard';
import { Role } from '../roles';
import { ListEventsDto } from './dto/list-events.dto';
import { EventLogService } from './event-log.service';

@Controller('events')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
@Roles(Role.Admin, Role.Compliance, Role.Security, Role.SuperAdmin)
export class EventLogController {
  constructor(private readonly events: EventLogService) {}

  @Get()
  async list(@Query() query: ListEventsDto, @Req() req: Request) {
    const sessionUser = (req as any).session?.user || (req as any).user;
    const role = (sessionUser?.role as Role) || Role.User;
    if (role === Role.OrgOwner) {
      throw new ForbiddenException('Insufficient role for audit logs');
    }
    const orgId = sessionUser?.orgId;
    const scopeOrg = role === Role.SuperAdmin ? query.orgId || orgId : orgId;
    if (!scopeOrg) {
      throw new ForbiddenException('Organization context required');
    }
    return this.events.list(scopeOrg, query, role);
  }

  @Get(':id')
  async getOne(@Param('id') id: string, @Query('orgId') orgIdParam: string | undefined, @Req() req: Request) {
    const sessionUser = (req as any).session?.user || (req as any).user;
    const role = (sessionUser?.role as Role) || Role.User;
    if (role === Role.OrgOwner) {
      throw new ForbiddenException('Insufficient role for audit logs');
    }
    const orgId = role === Role.SuperAdmin ? orgIdParam || sessionUser?.orgId : sessionUser?.orgId;
    if (!orgId) {
      throw new ForbiddenException('Organization context required');
    }
    return this.events.findByIdScoped(id, orgId, role, orgIdParam);
  }
}
