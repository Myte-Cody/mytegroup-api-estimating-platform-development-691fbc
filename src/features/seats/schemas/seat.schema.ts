import { Document, Schema } from 'mongoose';

export type SeatStatus = 'vacant' | 'active';

export interface Seat extends Document {
  orgId: string;
  seatNumber: number;
  status: SeatStatus;
  userId?: string;
  activatedAt?: Date | null;
  createdAt: Date;
  updatedAt: Date;
}

export const SeatSchema = new Schema<Seat>(
  {
    orgId: { type: String, required: true, index: true },
    seatNumber: { type: Number, required: true },
    status: { type: String, enum: ['vacant', 'active'], default: 'vacant', index: true },
    userId: {
      type: String,
      set: (value: unknown) => {
        if (typeof value !== 'string') return undefined;
        const trimmed = value.trim();
        return trimmed ? trimmed : undefined;
      },
    },
    activatedAt: { type: Date, default: null },
  },
  { timestamps: true }
);

SeatSchema.index({ orgId: 1, seatNumber: 1 }, { unique: true });
SeatSchema.index({ orgId: 1, status: 1 });
SeatSchema.index({ orgId: 1, userId: 1 }, { unique: true, sparse: true });

