import {
  BadRequestException,
  Body,
  Controller,
  Get,
  HttpException,
  HttpStatus,
  Param,
  Post,
  Query,
  Req,
  Res,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { Express, Request, Response } from 'express';
import { memoryStorage } from 'multer';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { storageConfig } from '../../config/app.config';
import { getRateLimitExceededBody } from './bulk.rate-limit';
import { BulkEntityType, BulkService } from './bulk.service';

const ALLOWED_ENTITIES: BulkEntityType[] = ['users', 'contacts', 'projects', 'offices'];

@Controller()
export class BulkController {
  constructor(private readonly bulk: BulkService) {}

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(
    Role.SuperAdmin,
    Role.PlatformAdmin,
    Role.Admin,
    Role.OrgOwner,
    Role.OrgAdmin,
    Role.Compliance,
    Role.ComplianceOfficer
  )
  @Post('bulk-import/:entityType')
  @UseInterceptors(
    FileInterceptor('file', {
      storage: memoryStorage(),
      limits: { fileSize: storageConfig.maxUploadBytes },
    })
  )
  async import(
    @Param('entityType') entityTypeParam: string,
    @UploadedFile() file: Express.Multer.File,
    @Body('records') records: any[],
    @Body() body: any,
    @Query('dryRun') dryRun?: string,
    @Query('orgId') orgId?: string,
    @Req() req?: Request
  ) {
    const entityType = entityTypeParam?.toLowerCase() as BulkEntityType;
    if (!ALLOWED_ENTITIES.includes(entityType)) {
      throw new BadRequestException('Unsupported entity type');
    }
    if (
      file &&
      ![
        'text/csv',
        'application/csv',
        'application/json',
        'text/json',
        'text/plain',
      ].some((type) => file.mimetype?.includes(type.split('/')[1]) || file.mimetype === type)
    ) {
      throw new BadRequestException('Unsupported file type. Only CSV or JSON are allowed.');
    }
    // Propagate consistent rate-limit body when express-rate-limit sets a status
    if ((req as any).rateLimit?.current > (req as any).rateLimit?.limit) {
      throw new HttpException(getRateLimitExceededBody(), HttpStatus.TOO_MANY_REQUESTS);
    }
    const actor = req?.session?.user || {};
    const payloadRecords = Array.isArray(records)
      ? records
      : Array.isArray(body?.data)
        ? body.data
        : undefined;
    const formatHint = body?.format === 'csv' || body?.format === 'json' ? body.format : undefined;
    const result = await this.bulk.import(entityType, {
      entityType,
      orgId,
      actor,
      dryRun: ['true', '1'].includes(String(dryRun || '').toLowerCase()),
      records: payloadRecords,
      file,
      filename: file?.originalname,
      formatHint,
    });
    return result;
  }

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(
    Role.SuperAdmin,
    Role.PlatformAdmin,
    Role.Admin,
    Role.OrgOwner,
    Role.OrgAdmin,
    Role.Compliance,
    Role.ComplianceOfficer
  )
  @Get('bulk-export/:entityType')
  async export(
    @Param('entityType') entityTypeParam: string,
    @Query('format') format?: string,
    @Query('includeArchived') includeArchived?: string,
    @Query('orgId') orgId?: string,
    @Req() req?: Request,
    @Res() res?: Response
  ) {
    const entityType = entityTypeParam?.toLowerCase() as BulkEntityType;
    if (!ALLOWED_ENTITIES.includes(entityType)) {
      throw new BadRequestException('Unsupported entity type');
    }
    const actor = req?.session?.user || {};
    const result = await this.bulk.export(entityType, {
      entityType,
      orgId,
      actor,
      format: format === 'csv' ? 'csv' : 'json',
      includeArchived: ['true', '1'].includes(String(includeArchived || '').toLowerCase()),
    });
    if (result.format === 'csv') {
      res?.setHeader('Content-Type', 'text/csv');
      res?.setHeader('Content-Disposition', `attachment; filename=\"${result.filename}\"`);
      await new Promise<void>((resolve, reject) => {
        result.stream.on('error', reject);
        (res as Response)?.on?.('error', reject);
        (res as Response)?.on?.('finish', () => resolve());
        result.stream.pipe(res as Response);
      });
      return;
    }
    if (res) {
      res.json?.(result);
      return;
    }
    return result;
  }
}
