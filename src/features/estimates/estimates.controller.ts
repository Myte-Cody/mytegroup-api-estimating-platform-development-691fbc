import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  Query,
  Req,
  UseGuards,
} from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { CreateEstimateDto } from './dto/create-estimate.dto';
import { UpdateEstimateDto } from './dto/update-estimate.dto';
import { EstimatesService } from './estimates.service';

@Controller('projects/:projectId/estimates')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class EstimatesController {
  constructor(private readonly estimates: EstimatesService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  private parseIncludeArchived(raw?: string) {
    return raw === 'true' || raw === '1';
  }

  @Roles(Role.Estimator, Role.PM, Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Post()
  async create(@Param('projectId') projectId: string, @Body() dto: CreateEstimateDto, @Req() req: Request) {
    return this.estimates.create(projectId, dto, this.actor(req));
  }

  @Roles(Role.Estimator, Role.PM, Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Get()
  async list(
    @Param('projectId') projectId: string,
    @Req() req: Request,
    @Query('includeArchived') includeArchived?: string,
  ) {
    return this.estimates.list(projectId, this.actor(req), this.parseIncludeArchived(includeArchived));
  }

  @Roles(Role.Estimator, Role.PM, Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Get(':estimateId')
  async getOne(
    @Param('projectId') projectId: string,
    @Param('estimateId') estimateId: string,
    @Req() req: Request,
    @Query('includeArchived') includeArchived?: string,
  ) {
    return this.estimates.getById(projectId, estimateId, this.actor(req), this.parseIncludeArchived(includeArchived));
  }

  @Roles(Role.Estimator, Role.PM, Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Patch(':estimateId')
  async update(
    @Param('projectId') projectId: string,
    @Param('estimateId') estimateId: string,
    @Body() dto: UpdateEstimateDto,
    @Req() req: Request
  ) {
    return this.estimates.update(projectId, estimateId, dto, this.actor(req));
  }

  @Roles(Role.Estimator, Role.PM, Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Post(':estimateId/archive')
  async archive(@Param('projectId') projectId: string, @Param('estimateId') estimateId: string, @Req() req: Request) {
    return this.estimates.archive(projectId, estimateId, this.actor(req));
  }

  @Roles(Role.Estimator, Role.PM, Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Post(':estimateId/unarchive')
  async unarchive(
    @Param('projectId') projectId: string,
    @Param('estimateId') estimateId: string,
    @Req() req: Request
  ) {
    return this.estimates.unarchive(projectId, estimateId, this.actor(req));
  }
}
