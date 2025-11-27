import { Body, Controller, Get, Post, Query, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { BatchArchiveDto } from './dto/batch-archive.dto';
import { SetLegalHoldDto } from './dto/set-legal-hold.dto';
import { StripPiiDto } from './dto/strip-pii.dto';
import { ComplianceService } from './compliance.service';

const COMPLIANCE_ALLOWED_ROLES = [
  Role.SuperAdmin,
  Role.PlatformAdmin,
  Role.Admin,
  Role.OrgOwner,
  Role.OrgAdmin,
  Role.Compliance,
  Role.ComplianceOfficer,
  Role.SecurityOfficer,
  Role.Security,
];

@Controller('compliance')
export class ComplianceController {
  constructor(private readonly compliance: ComplianceService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { id: user?.id, orgId: user?.orgId, role: user?.role };
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(...COMPLIANCE_ALLOWED_ROLES)
  @Post('strip-pii')
  stripPii(@Body() dto: StripPiiDto, @Req() req: Request) {
    return this.compliance.stripPii(dto, this.actor(req));
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(...COMPLIANCE_ALLOWED_ROLES)
  @Post('legal-hold')
  setLegalHold(@Body() dto: SetLegalHoldDto, @Req() req: Request) {
    return this.compliance.setLegalHold(dto, this.actor(req));
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(...COMPLIANCE_ALLOWED_ROLES)
  @Post('batch-archive')
  batchArchive(@Body() dto: BatchArchiveDto, @Req() req: Request) {
    return this.compliance.batchArchive(dto, this.actor(req));
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(...COMPLIANCE_ALLOWED_ROLES)
  @Get('status')
  status(@Query('entityType') entityType: string, @Query('entityId') entityId: string, @Req() req: Request) {
    return this.compliance.status(entityType, entityId, this.actor(req));
  }
}
