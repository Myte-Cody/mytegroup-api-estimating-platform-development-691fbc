import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Put,
  Query,
  Req,
  Res,
  StreamableFile,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { memoryStorage } from 'multer';
import { Request, Response } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { storageConfig } from '../../config/app.config';
import { BulkCostCodesDto } from './dto/bulk-cost-codes.dto';
import { CreateCostCodeDto } from './dto/create-cost-code.dto';
import { CostCodeImportCommitDto } from './dto/import-commit.dto';
import { ListCostCodesQueryDto } from './dto/list-cost-codes.dto';
import { SeedCostCodesDto } from './dto/seed-cost-codes.dto';
import { ToggleCostCodeDto } from './dto/toggle-cost-code.dto';
import { UpdateCostCodeDto } from './dto/update-cost-code.dto';
import { CostCodesService } from './cost-codes.service';

@Controller('cost-codes')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class CostCodesController {
  constructor(private readonly svc: CostCodesService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Get()
  list(@Query() query: ListCostCodesQueryDto, @Req() req: Request) {
    return this.svc.list(this.actor(req), query);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Get('counts')
  counts(@Query() query: ListCostCodesQueryDto, @Req() req: Request) {
    return this.svc.counts(this.actor(req), query);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Get('template')
  async template(@Req() req: Request, @Res({ passthrough: true }) res: Response) {
    const buffer = await this.svc.buildTemplate(this.actor(req));
    res.set({
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'Content-Disposition': 'attachment; filename="cost-codes-template.xlsx"',
    });
    return new StreamableFile(buffer);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Post('seed')
  seed(@Body() dto: SeedCostCodesDto, @Req() req: Request) {
    return this.svc.seedPack(this.actor(req), dto);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Get(':id')
  getById(@Param('id') id: string, @Req() req: Request) {
    return this.svc.getById(this.actor(req), id);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Post()
  create(@Body() dto: CreateCostCodeDto, @Req() req: Request) {
    return this.svc.create(this.actor(req), dto);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Put(':id')
  update(@Param('id') id: string, @Body() dto: UpdateCostCodeDto, @Req() req: Request) {
    return this.svc.update(this.actor(req), id, dto);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Patch(':id/active')
  toggle(@Param('id') id: string, @Body() dto: ToggleCostCodeDto, @Req() req: Request) {
    return this.svc.toggleActive(this.actor(req), id, dto);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Delete(':id')
  remove(@Param('id') id: string, @Req() req: Request) {
    return this.svc.remove(this.actor(req), id);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Post('bulk')
  bulk(@Body() dto: BulkCostCodesDto, @Req() req: Request) {
    return this.svc.bulkUpsert(this.actor(req), dto);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Post('import/analyze')
  analyze(@Req() req: Request) {
    return this.svc.analyzeImport(this.actor(req));
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Post('import')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: memoryStorage(),
      limits: { fileSize: storageConfig.maxUploadBytes },
    })
  )
  startImport(@UploadedFile() file: Express.Multer.File, @Req() req: Request) {
    return this.svc.startImport(this.actor(req), file);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Get('import/:jobId')
  status(@Param('jobId') jobId: string, @Req() req: Request) {
    return this.svc.importStatus(this.actor(req), jobId);
  }

  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.OrgOwner, Role.OrgAdmin)
  @Post('import/:jobId/commit')
  commit(@Param('jobId') jobId: string, @Body() dto: CostCodeImportCommitDto, @Req() req: Request) {
    return this.svc.commitImport(this.actor(req), jobId, dto);
  }
}
