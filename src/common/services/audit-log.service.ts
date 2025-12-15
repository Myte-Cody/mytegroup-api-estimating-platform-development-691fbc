import { Injectable } from '@nestjs/common';
import { ClientSession } from 'mongoose';
import { EventLogService } from '../events/event-log.service';

@Injectable()
export class AuditLogService {
  constructor(private readonly eventLog: EventLogService) {}

  async log(event: string | { eventType: string; [key: string]: any }, options?: { session?: ClientSession }) {
    if (typeof event === 'string') {
      await this.eventLog.log({ eventType: event, metadata: { message: event } }, options);
      return;
    }
    await this.eventLog.log(event, options);
  }

  async logMutation(params: {
    action: string;
    entity: string;
    entityId?: string;
    orgId?: string;
    userId?: string;
    metadata?: Record<string, any>;
    payload?: Record<string, any>;
  }, options?: { session?: ClientSession }) {
    const { action, entity, entityId, orgId, userId, metadata, payload } = params;
    await this.eventLog.log({
      eventType: `${entity}.${action}`,
      entity,
      entityId,
      orgId,
      userId,
      metadata,
      payload,
    }, options);
  }
}
