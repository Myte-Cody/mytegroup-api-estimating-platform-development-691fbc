import { Body, Controller, Get, Post, Query, Req, UseGuards } from '@nestjs/common'
import { Request } from 'express'
import { Roles } from '../../common/decorators/roles.decorator'
import { RolesGuard } from '../../common/guards/roles.guard'
import { SessionGuard } from '../../common/guards/session.guard'
import { Role } from '../../common/roles'
import { ApproveWaitlistDto } from './dto/approve-waitlist.dto'
import { InviteBatchDto } from './dto/invite-batch.dto'
import { WaitlistEventDto } from './dto/waitlist-event.dto'
import { StartWaitlistDto } from './dto/start-waitlist.dto'
import { VerifyWaitlistDto } from './dto/verify-waitlist.dto'
import { ResendWaitlistDto } from './dto/resend-waitlist.dto'
import { ListWaitlistDto } from './dto/list-waitlist.dto'
import { WaitlistService } from './waitlist.service'

@Controller('marketing')
export class WaitlistController {
  constructor(private readonly waitlist: WaitlistService) {}

  @Post('waitlist')
  async create(@Body() dto: StartWaitlistDto, @Req() req: Request) {
    if (dto.trap) return { status: 'ok' }
    const result = await this.waitlist.start(dto, req.ip)
    return { status: result.status, entry: result.entry }
  }

  @Post('waitlist/verify')
  async verify(@Body() dto: VerifyWaitlistDto) {
    const entry = await this.waitlist.verify(dto)
    return { status: 'verified', entry }
  }

  @Post('waitlist/resend')
  async resend(@Body() dto: ResendWaitlistDto, @Req() req: Request) {
    const result = await this.waitlist.resend(dto, req.ip)
    return result
  }

  @Get('waitlist/stats')
  async stats() {
    return this.waitlist.stats()
  }

  @Post('events')
  async events(@Body() dto: WaitlistEventDto, @Req() req: Request) {
    await this.waitlist.assertEventAllowed(req.ip)
    await this.waitlist.logEvent(dto.event, { meta: dto.meta, source: dto.source, path: dto.path })
    return { status: 'ok' }
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Get('waitlist')
  async listWaitlist(@Query() query: ListWaitlistDto) {
    const { page, limit, status, verifyStatus, cohortTag, emailContains } = query
    return this.waitlist.list({ page, limit, status, verifyStatus, cohortTag, emailContains })
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Post('waitlist/invite-batch')
  async inviteBatch(@Body() dto: InviteBatchDto) {
    return this.waitlist.processInviteBatch(dto.limit, dto.cohortTag)
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  @Post('waitlist/approve')
  async approve(@Body() dto: ApproveWaitlistDto) {
    await this.waitlist.markInvited(dto.email, dto.cohortTag)
    return { status: 'ok' }
  }
}
