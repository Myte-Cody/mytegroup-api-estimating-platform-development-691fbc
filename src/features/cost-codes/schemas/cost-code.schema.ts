import { Document, Schema } from 'mongoose';

export interface CostCode extends Document {
  orgId: string;
  category: string;
  code: string;
  description: string;
  active: boolean;
  deactivatedAt?: Date | null;
  isUsed: boolean;
  importJobId?: string | null;
  createdAt: Date;
  updatedAt: Date;
}

export const CostCodeSchema = new Schema<CostCode>(
  {
    orgId: { type: String, required: true, index: true },
    category: { type: String, required: true, index: true },
    code: { type: String, required: true },
    description: { type: String, required: true },
    active: { type: Boolean, default: false, index: true },
    deactivatedAt: { type: Date, default: null },
    isUsed: { type: Boolean, default: false },
    importJobId: { type: String, default: null, index: true },
  },
  { timestamps: true }
);

CostCodeSchema.index({ orgId: 1, code: 1 }, { unique: true });
