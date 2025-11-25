import { Injectable } from '@nestjs/common';
import { EventLogService } from '../events/event-log.service';

@Injectable()
export class AuditLogService {
  constructor(private readonly eventLog: EventLogService) {}

  async log(event: string | { eventType: string; [key: string]: any }) {
    if (typeof event === 'string') {
      await this.eventLog.log({ eventType: event, metadata: { message: event } });
      return;
    }
    await this.eventLog.log(event);
  }
}
