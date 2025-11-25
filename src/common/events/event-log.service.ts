import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { EventLog } from './event-log.schema';
import { loggingConfig } from '../../config/app.config';

@Injectable()
export class EventLogService {
  private readonly logger = new Logger(EventLogService.name);
  constructor(@InjectModel('EventLog') private readonly model: Model<EventLog>) {}

  async log(event: Partial<EventLog>) {
    const payload = {
      ...event,
      createdAt: event.createdAt || new Date(),
      metadata: loggingConfig.sanitizeMetadata(event.metadata),
    };
    try {
      await this.model.create(payload);
    } catch (err) {
      this.logger.error(`Failed to persist event ${event.eventType || 'unknown'}`, err as Error);
    }
    if (loggingConfig.enableConsole) {
      this.logger.log(`EVENT: ${event.eventType || 'message'}`);
    }
  }
}
