import { Controller, Post, Body, UseGuards, Patch, Param, Req, ForbiddenException } from '@nestjs/common';
import { OrganizationsService } from './organizations.service';
import { CreateOrganizationDto } from './dto/create-organization.dto';
import { SessionGuard } from '../../common/guards/session.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../common/roles';
import { UpdateOrganizationDatastoreDto } from './dto/update-organization-datastore.dto';
import { Request } from 'express';
@Controller('organizations')
export class OrganizationsController {
  constructor(private svc: OrganizationsService) {}
  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Post()
  create(@Body() dto: CreateOrganizationDto) { return this.svc.create(dto); }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Patch(':id/archive')
  archive(@Param('id') id: string) {
    return this.svc.archive(id);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin)
  @Patch(':id/unarchive')
  unarchive(@Param('id') id: string) {
    return this.svc.unarchive(id);
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin, Role.OrgOwner, Role.Admin)
  @Patch(':id/datastore')
  updateDatastore(@Param('id') id: string, @Body() dto: UpdateOrganizationDatastoreDto, @Req() req: Request) {
    const actor = req.session?.user;
    if (actor?.role !== Role.SuperAdmin && actor?.orgId !== id) {
      throw new ForbiddenException('Cannot modify datastore for another organization');
    }
    return this.svc.updateDatastore(id, dto);
  }
}
