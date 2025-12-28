import { Document, Schema } from 'mongoose';

export interface Notification extends Document {
  orgId: string;
  userId: string;
  type: string;
  payload?: Record<string, any>;
  read: boolean;
  readAt?: Date | null;
  createdAt: Date;
  updatedAt: Date;
}

export const NotificationSchema = new Schema<Notification>(
  {
    orgId: { type: String, required: true, index: true },
    userId: { type: String, required: true, index: true },
    type: { type: String, required: true },
    payload: { type: Schema.Types.Mixed, default: {} },
    read: { type: Boolean, default: false, index: true },
    readAt: { type: Date, default: null },
  },
  { timestamps: true }
);

NotificationSchema.index({ orgId: 1, userId: 1, read: 1, createdAt: -1 });
