import { Schema, Document } from 'mongoose';

export interface EventLog extends Document {
  eventType: string;
  action?: string;
  entity?: string;
  entityType?: string;
  entityId?: string;
  userId?: string;
  actor?: string;
  orgId?: string;
  payload?: Record<string, unknown>;
  metadata?: Record<string, unknown>;
  ipAddress?: string;
  sessionId?: string;
  requestId?: string;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
}

export const EventLogSchema = new Schema<EventLog>(
  {
    eventType: { type: String, required: true, index: true },
    action: { type: String, index: true },
    entity: { type: String },
    entityType: { type: String, index: true },
    entityId: { type: String, index: true },
    userId: { type: String },
    actor: { type: String, index: true },
    orgId: { type: String, index: true },
    payload: { type: Schema.Types.Mixed },
    metadata: { type: Schema.Types.Mixed },
    ipAddress: { type: String },
    sessionId: { type: String },
    requestId: { type: String },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { versionKey: false, timestamps: { createdAt: true, updatedAt: false } }
);

EventLogSchema.index({ orgId: 1, createdAt: -1 });
EventLogSchema.index({ orgId: 1, entityId: 1, createdAt: -1 });
EventLogSchema.index({ orgId: 1, action: 1, createdAt: -1 });
EventLogSchema.index({ orgId: 1, eventType: 1, createdAt: -1 });
EventLogSchema.index(
  { createdAt: 1 },
  { expireAfterSeconds: 60 * 60 * 24 * 365 * 10, partialFilterExpression: { legalHold: { $ne: true } } }
); // ~10 years retention, skip TTL when legal hold is active
