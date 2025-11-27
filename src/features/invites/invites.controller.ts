import { Body, Controller, Get, Param, Post, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role, resolvePrimaryRole } from '../../common/roles';
import { AcceptInviteDto } from './dto/accept-invite.dto';
import { CreateInviteDto } from './dto/create-invite.dto';
import { InvitesService } from './invites.service';

@Controller('invites')
export class InvitesController {
  constructor(private readonly invites: InvitesService) {}

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.OrgOwner, Role.Admin)
  @Post()
  async create(@Body() dto: CreateInviteDto, @Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    const actorId = req.session?.user?.id;
    const actorRole = resolvePrimaryRole((req.session?.user?.roles as Role[]) || [req.session?.user?.role as Role]);
    return this.invites.create(orgId, actorId, actorRole, dto);
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.OrgOwner, Role.Admin)
  @Get()
  async list(@Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    return this.invites.list(orgId);
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.OrgOwner, Role.Admin)
  @Post(':id/resend')
  async resend(@Param('id') id: string, @Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    const actorId = req.session?.user?.id;
    return this.invites.resend(orgId, id, actorId);
  }

  @Post('accept')
  async accept(@Body() dto: AcceptInviteDto) {
    return this.invites.accept(dto);
  }
}
