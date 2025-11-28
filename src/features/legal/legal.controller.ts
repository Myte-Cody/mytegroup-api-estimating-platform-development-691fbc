import { BadRequestException, Body, Controller, Get, Param, ParseEnumPipe, Post, Query, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { Roles } from '../../common/decorators/roles.decorator';
import { RolesGuard } from '../../common/guards/roles.guard';
import { SessionGuard } from '../../common/guards/session.guard';
import { Role } from '../../common/roles';
import { AcceptLegalDocDto } from './dto/accept-legal-doc.dto';
import { CreateLegalDocDto } from './dto/create-legal-doc.dto';
import { LegalDocType } from './legal.types';
import { LegalService } from './legal.service';

@Controller('legal')
export class LegalController {
  constructor(private readonly legal: LegalService) {}

  private actor(req: Request) {
    const user = (req as any).user || req.session?.user || {};
    return { id: user.id, orgId: user.orgId };
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin, Role.PlatformAdmin)
  @Post()
  async create(@Body() dto: CreateLegalDocDto, @Req() req: Request) {
    return this.legal.createDoc(dto, this.actor(req));
  }

  @Get(':type')
  async latest(@Param('type', new ParseEnumPipe(LegalDocType)) type: LegalDocType) {
    return this.legal.latestDoc(type);
  }

  @Get(':type/history')
  async history(
    @Param('type', new ParseEnumPipe(LegalDocType)) type: LegalDocType,
    @Query('limit') limit?: string
  ) {
    const parsed = limit ? Number(limit) : undefined;
    if (parsed !== undefined && (Number.isNaN(parsed) || parsed < 1)) {
      throw new BadRequestException('limit must be a positive number');
    }
    return this.legal.history(type, parsed || 10);
  }

  @UseGuards(SessionGuard)
  @Get('acceptance/status')
  async status(@Req() req: Request) {
    return this.legal.acceptanceStatus(this.actor(req));
  }

  @UseGuards(SessionGuard)
  @Post('accept')
  async accept(@Body() dto: AcceptLegalDocDto, @Req() req: Request) {
    const meta = {
      ipAddress: (req.headers['x-forwarded-for'] as string) || (req.socket?.remoteAddress as string),
      userAgent: req.headers['user-agent'] as string,
    };
    return this.legal.accept(dto, this.actor(req), meta);
  }
}
