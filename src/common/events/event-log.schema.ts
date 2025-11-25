import { Schema, Document } from 'mongoose';

export interface EventLog extends Document {
  eventType: string;
  entity?: string;
  entityId?: string;
  userId?: string;
  orgId?: string;
  metadata?: Record<string, unknown>;
  createdAt: Date;
}

export const EventLogSchema = new Schema<EventLog>(
  {
    eventType: { type: String, required: true },
    entity: { type: String },
    entityId: { type: String },
    userId: { type: String },
    orgId: { type: String },
    metadata: { type: Schema.Types.Mixed },
    createdAt: { type: Date, default: Date.now },
  },
  { versionKey: false }
);
