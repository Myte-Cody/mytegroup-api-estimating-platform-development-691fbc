import { Body, Controller, Post, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { CompaniesImportConfirmDto, CompaniesImportPreviewDto } from './dto/companies-import.dto';
import { CompaniesImportService } from './companies-import.service';

@Controller('companies/import/v1')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class CompaniesImportController {
  constructor(private readonly imports: CompaniesImportService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('preview')
  preview(@Body() dto: CompaniesImportPreviewDto, @Req() req: Request) {
    return this.imports.preview(this.actor(req), dto.rows);
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('confirm')
  confirm(@Body() dto: CompaniesImportConfirmDto, @Req() req: Request) {
    return this.imports.confirm(this.actor(req), dto.rows);
  }
}

