import { Body, Controller, Get, Post, Req, UseGuards } from '@nestjs/common'
import { Request } from 'express'
import { SessionGuard } from '../../common/guards/session.guard'
import { SessionsService } from './sessions.service'
import { RevokeSessionDto } from './dto/revoke-session.dto'

@Controller('sessions')
@UseGuards(SessionGuard)
export class SessionsController {
  constructor(private readonly sessions: SessionsService) {}

  @Get('me')
  async list(@Req() req: Request) {
    const userId = req.session.user?.id
    const sessions = await this.sessions.listSessions(userId, req.sessionID)
    return { sessions }
  }

  @Post('revoke')
  async revoke(@Req() req: Request, @Body() dto: RevokeSessionDto) {
    const userId = req.session.user?.id
    await this.sessions.revokeSession(dto.sessionId, userId)
    // If the current session is revoked, also destroy it
    if (dto.sessionId === req.sessionID) {
      await new Promise((resolve) => req.session.destroy(() => resolve(null)))
    }
    return { status: 'revoked' }
  }

  @Post('logout-all')
  async logoutAll(@Req() req: Request) {
    const userId = req.session.user?.id
    await this.sessions.revokeAll(userId)
    await new Promise((resolve) => req.session.destroy(() => resolve(null)))
    return { status: 'revoked' }
  }
}
