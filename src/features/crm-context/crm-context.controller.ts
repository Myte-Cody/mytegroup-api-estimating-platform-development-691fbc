import { Controller, Get, Query, Req, UseGuards, ForbiddenException } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { ListCrmContextDocumentsQueryDto } from './dto/list-crm-context-documents.dto';
import { CrmContextService } from './crm-context.service';

@Controller('crm-context')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class CrmContextController {
  constructor(private readonly svc: CrmContextService) {}

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
  @Get('documents')
  documents(@Query() query: ListCrmContextDocumentsQueryDto, @Req() req: Request) {
    const actor = this.actor(req);
    const resolvedOrgId = this.resolveOrgId(actor.orgId, actor.role, query.orgId);
    return this.svc.listDocuments(actor, resolvedOrgId, query);
  }
}

