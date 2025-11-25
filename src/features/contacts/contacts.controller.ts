import { BadRequestException, Body, Controller, Get, Param, Patch, Post, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { CreateContactDto } from './dto/create-contact.dto';
import { UpdateContactDto } from './dto/update-contact.dto';
import { ContactsService } from './contacts.service';

@Controller('contacts')
@UseGuards(SessionGuard, OrgScopeGuard)
export class ContactsController {
  constructor(private readonly contacts: ContactsService) {}

  @Get()
  async list(@Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.list(orgId);
  }

  @UseGuards(RolesGuard)
  @Roles(Role.OrgOwner, Role.Admin, Role.SuperAdmin, Role.PM)
  @Post()
  async create(@Body() dto: CreateContactDto, @Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.create(orgId, dto);
  }

  @UseGuards(RolesGuard)
  @Roles(Role.OrgOwner, Role.Admin, Role.SuperAdmin, Role.PM)
  @Patch(':id')
  async update(@Param('id') id: string, @Body() dto: UpdateContactDto, @Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.update(orgId, id, dto);
  }

  @UseGuards(RolesGuard)
  @Roles(Role.OrgOwner, Role.Admin, Role.SuperAdmin)
  @Patch(':id/archive')
  async archive(@Param('id') id: string, @Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.archive(orgId, id);
  }

  @UseGuards(RolesGuard)
  @Roles(Role.OrgOwner, Role.Admin, Role.SuperAdmin)
  @Patch(':id/unarchive')
  async unarchive(@Param('id') id: string, @Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    if (!orgId) throw new BadRequestException('Organization context is required');
    return this.contacts.unarchive(orgId, id);
  }
}
