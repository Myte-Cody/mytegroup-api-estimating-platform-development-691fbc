import { Body, Controller, Get, Param, Post, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { AbortMigrationDto } from './dto/abort-migration.dto';
import { FinalizeMigrationDto } from './dto/finalize-migration.dto';
import { StartMigrationDto } from './dto/start-migration.dto';
import { MigrationsService } from './migrations.service';

@Controller('migration')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
@Roles(Role.SuperAdmin)
export class MigrationsController {
  constructor(private readonly migrations: MigrationsService) {}

  @Post('start')
  async start(@Body() dto: StartMigrationDto, @Req() req: Request) {
    const actor = { id: req.session?.user?.id, role: req.session?.user?.role };
    return this.migrations.start(dto, actor);
  }

  @Get('status/:orgId')
  async status(@Param('orgId') orgId: string) {
    return this.migrations.status(orgId);
  }

  @Post('abort')
  async abort(@Body() dto: AbortMigrationDto, @Req() req: Request) {
    const actor = { id: req.session?.user?.id, role: req.session?.user?.role };
    return this.migrations.abort(dto, actor);
  }

  @Post('finalize')
  async finalize(@Body() dto: FinalizeMigrationDto, @Req() req: Request) {
    const actor = { id: req.session?.user?.id, role: req.session?.user?.role };
    return this.migrations.finalize(dto, actor);
  }
}
