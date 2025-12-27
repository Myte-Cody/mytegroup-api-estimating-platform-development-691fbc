import { Body, Controller, Post, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { PeopleImportConfirmDto, PeopleImportPreviewDto } from './dto/people-import.dto';
import { PeopleImportV1ConfirmDto, PeopleImportV1PreviewDto } from './dto/people-import-v1.dto';
import { PeopleImportService } from './people-import.service';

@Controller('people/import')
@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
export class PeopleController {
  constructor(private readonly peopleImport: PeopleImportService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('preview')
  preview(@Body() dto: PeopleImportPreviewDto, @Req() req: Request) {
    return this.peopleImport.preview(this.actor(req), dto.rows);
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('v1/preview')
  previewV1(@Body() dto: PeopleImportV1PreviewDto, @Req() req: Request) {
    return this.peopleImport.previewV1(this.actor(req), dto.rows);
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('confirm')
  confirm(@Body() dto: PeopleImportConfirmDto, @Req() req: Request) {
    return this.peopleImport.confirm(this.actor(req), dto.rows);
  }

  @Roles(Role.Admin, Role.OrgOwner, Role.OrgAdmin, Role.SuperAdmin)
  @Post('v1/confirm')
  confirmV1(@Body() dto: PeopleImportV1ConfirmDto, @Req() req: Request) {
    return this.peopleImport.confirmV1(this.actor(req), dto.rows);
  }
}
