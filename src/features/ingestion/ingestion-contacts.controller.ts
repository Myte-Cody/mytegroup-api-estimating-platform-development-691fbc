import { Body, Controller, Post, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { IngestionContactsEnrichDto } from './dto/ingestion-contacts-enrich.dto';
import { IngestionContactsParseRowDto } from './dto/ingestion-contacts-parse-row.dto';
import { IngestionContactsSuggestMappingDto } from './dto/ingestion-contacts-suggest-mapping.dto';
import { IngestionContactsService } from './ingestion-contacts.service';

@Controller('ingestion/contacts/v1')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class IngestionContactsController {
  constructor(private readonly ingestion: IngestionContactsService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('suggest-mapping')
  suggestMapping(@Body() dto: IngestionContactsSuggestMappingDto, @Req() req: Request) {
    return this.ingestion.suggestMapping(this.actor(req), dto);
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('parse-row')
  parseRow(@Body() dto: IngestionContactsParseRowDto, @Req() req: Request) {
    return this.ingestion.parseRow(this.actor(req), dto);
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('enrich')
  enrich(@Body() dto: IngestionContactsEnrichDto, @Req() req: Request) {
    return this.ingestion.enrich(this.actor(req), dto);
  }
}
