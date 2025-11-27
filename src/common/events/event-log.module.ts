import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { OrgScopeGuard } from '../guards/org-scope.guard';
import { RolesGuard } from '../guards/roles.guard';
import { SessionGuard } from '../guards/session.guard';
import { EventLogController } from './event-log.controller';
import { EventLogSchema } from './event-log.schema';
import { EventLogService } from './event-log.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: 'EventLog', schema: EventLogSchema }])],
  controllers: [EventLogController],
  providers: [EventLogService, SessionGuard, OrgScopeGuard, RolesGuard],
  exports: [EventLogService],
})
export class EventLogModule {}
