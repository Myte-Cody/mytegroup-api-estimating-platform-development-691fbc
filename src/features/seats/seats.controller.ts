import { Controller, Get, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { seatConfig } from '../../config/app.config';
import { SeatsService } from './seats.service';

@Controller('seats')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class SeatsController {
  constructor(private readonly seats: SeatsService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin, Role.PlatformAdmin)
  @Get('summary')
  async summary(@Req() req: Request) {
    const actor = this.actor(req);
    if (!actor.orgId) return { orgId: null, total: 0, active: 0, vacant: 0 };
    await this.seats.ensureOrgSeats(actor.orgId, seatConfig.defaultSeatsPerOrg);
    return this.seats.summary(actor.orgId);
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin, Role.PlatformAdmin)
  @Get()
  async list(@Req() req: Request) {
    const actor = this.actor(req);
    if (!actor.orgId) return [];
    await this.seats.ensureOrgSeats(actor.orgId, seatConfig.defaultSeatsPerOrg);
    return this.seats.list(actor.orgId);
  }
}

