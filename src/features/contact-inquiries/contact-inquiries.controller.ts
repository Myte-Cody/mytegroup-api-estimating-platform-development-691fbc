import { Body, Controller, Get, Param, Patch, Post, Query, Req, UseGuards } from '@nestjs/common'
import { Request } from 'express'
import { Roles } from '../../common/decorators/roles.decorator'
import { RolesGuard } from '../../common/guards/roles.guard'
import { SessionGuard } from '../../common/guards/session.guard'
import { Role } from '../../common/roles'
import { ContactInquiriesService } from './contact-inquiries.service'
import { ConfirmContactInquiryDto } from './dto/confirm-contact-inquiry.dto'
import { CreateContactInquiryDto } from './dto/create-contact-inquiry.dto'
import { ListContactInquiriesDto } from './dto/list-contact-inquiries.dto'
import { UpdateContactInquiryDto } from './dto/update-contact-inquiry.dto'
import { VerifyContactInquiryDto } from './dto/verify-contact-inquiry.dto'

@Controller('marketing/contact-inquiries')
export class ContactInquiriesController {
  constructor(private readonly contact: ContactInquiriesService) {}

  private getActor(req: Request) {
    const user = (req as any).user || req.session?.user
    return { userId: user?.id, role: user?.role as Role | undefined }
  }

  private getUserAgent(req: Request) {
    const ua = req.headers['user-agent']
    return Array.isArray(ua) ? ua.join('; ') : ua || null
  }

  @Post()
  async create(@Body() dto: CreateContactInquiryDto, @Req() req: Request) {
    const entry = await this.contact.create(dto, req.ip, this.getUserAgent(req))
    return { status: 'ok', entry }
  }

  @Post('verify-email')
  async verify(@Body() dto: VerifyContactInquiryDto, @Req() req: Request) {
    return this.contact.sendVerification(dto, req.ip)
  }

  @Post('verify-email/confirm')
  async confirm(@Body() dto: ConfirmContactInquiryDto) {
    return this.contact.confirmVerification(dto)
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.Compliance)
  @Get()
  async list(@Query() query: ListContactInquiriesDto) {
    const { status, page, limit } = query
    return this.contact.list({ status, page, limit })
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin, Role.PlatformAdmin, Role.Admin, Role.Compliance)
  @Patch(':id')
  async update(@Param('id') id: string, @Body() dto: UpdateContactInquiryDto, @Req() req: Request) {
    const actor = this.getActor(req)
    const entry = await this.contact.update(id, dto, actor.userId)
    return { status: 'ok', entry }
  }
}
