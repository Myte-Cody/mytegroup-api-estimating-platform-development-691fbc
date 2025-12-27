import { Body, Controller, Get, Param, Put, Query, Req, UseGuards, ForbiddenException } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { PutOrgTaxonomyDto } from './dto/put-org-taxonomy.dto';
import { OrgTaxonomyService } from './org-taxonomy.service';

@Controller('org-taxonomy')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class OrgTaxonomyController {
  constructor(private readonly svc: OrgTaxonomyService) {}

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
  @Get(':namespace')
  get(
    @Param('namespace') namespace: string,
    @Req() req: Request,
    @Query('orgId') orgId?: string
  ) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, orgId);
    return this.svc.get(actor, resolvedOrgId, namespace);
  }

  @Roles(Role.OrgOwner, Role.OrgAdmin, Role.Admin, Role.SuperAdmin, Role.PlatformAdmin)
  @Put(':namespace')
  put(
    @Param('namespace') namespace: string,
    @Body() dto: PutOrgTaxonomyDto,
    @Req() req: Request,
    @Query('orgId') orgId?: string
  ) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, orgId);
    return this.svc.put(actor, resolvedOrgId, namespace, dto.values);
  }
}
