import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { EventLogSchema } from './event-log.schema';
import { EventLogService } from './event-log.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: 'EventLog', schema: EventLogSchema }])],
  providers: [EventLogService],
  exports: [EventLogService],
})
export class EventLogModule {}
