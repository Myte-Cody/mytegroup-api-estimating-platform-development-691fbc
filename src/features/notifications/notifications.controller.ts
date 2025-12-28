import { Controller, Get, Param, Patch, Query, Req, UseGuards } from '@nestjs/common';
import { Request } from 'express';
import { SessionGuard } from '../../common/guards/session.guard';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { Role } from '../../common/roles';
import { ListNotificationsQueryDto } from './dto/list-notifications.dto';
import { NotificationsService } from './notifications.service';

@Controller('notifications')
@UseGuards(SessionGuard, OrgScopeGuard)
export class NotificationsController {
  constructor(private readonly notifications: NotificationsService) {}

  private getActor(req: Request) {
    const user = (req as any).user || req.session?.user;
    return { userId: user?.id, orgId: user?.orgId, role: user?.role as Role | undefined };
  }

  @Get()
  list(@Req() req: Request, @Query() query: ListNotificationsQueryDto) {
    const actor = this.getActor(req);
    return this.notifications.list(actor, query);
  }

  @Patch(':id/read')
  markRead(@Param('id') id: string, @Req() req: Request) {
    const actor = this.getActor(req);
    return this.notifications.markRead(actor, id);
  }
}
