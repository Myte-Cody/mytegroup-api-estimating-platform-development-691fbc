import { Document, Schema } from 'mongoose';

export type SeatStatus = 'vacant' | 'active';

export type SeatHistoryEntry = {
  userId?: string;
  projectId?: string;
  role?: string;
  assignedAt?: Date | null;
  removedAt?: Date | null;
};

export interface Seat extends Document {
  orgId: string;
  seatNumber: number;
  status: SeatStatus;
  role?: string | null;
  userId?: string;
  projectId?: string | null;
  activatedAt?: Date | null;
  history?: SeatHistoryEntry[];
  createdAt: Date;
  updatedAt: Date;
}

export const SeatSchema = new Schema<Seat>(
  {
    orgId: { type: String, required: true, index: true },
    seatNumber: { type: Number, required: true },
    status: { type: String, enum: ['vacant', 'active'], default: 'vacant', index: true },
    role: { type: String, default: null, index: true },
    userId: {
      type: String,
      set: (value: unknown) => {
        if (typeof value !== 'string') return undefined;
        const trimmed = value.trim();
        return trimmed ? trimmed : undefined;
      },
    },
    projectId: {
      type: String,
      set: (value: unknown) => {
        if (typeof value !== 'string') return undefined;
        const trimmed = value.trim();
        return trimmed ? trimmed : undefined;
      },
      default: null,
      index: true,
    },
    activatedAt: { type: Date, default: null },
    history: {
      type: [
        {
          userId: { type: String },
          projectId: { type: String },
          role: { type: String },
          assignedAt: { type: Date, default: null },
          removedAt: { type: Date, default: null },
        },
      ],
      default: [],
    },
  },
  { timestamps: true }
);

SeatSchema.index({ orgId: 1, seatNumber: 1 }, { unique: true });
SeatSchema.index({ orgId: 1, status: 1 });
SeatSchema.index({ orgId: 1, role: 1, status: 1 });
SeatSchema.index({ orgId: 1, projectId: 1 });
SeatSchema.index({ orgId: 1, userId: 1 }, { unique: true, sparse: true });
