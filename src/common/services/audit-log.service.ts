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

  async logMutation(params: {
    action: string;
    entity: string;
    entityId?: string;
    orgId?: string;
    userId?: string;
    metadata?: Record<string, any>;
    payload?: Record<string, any>;
  }) {
    const { action, entity, entityId, orgId, userId, metadata, payload } = params;
    await this.eventLog.log({
      eventType: `${entity}.${action}`,
      entity,
      entityId,
      orgId,
      userId,
      metadata,
      payload,
    });
  }
}
