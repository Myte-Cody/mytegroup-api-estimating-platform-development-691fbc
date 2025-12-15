import { Document, Schema } from 'mongoose';

export type EstimateStatus = 'draft' | 'final' | 'archived';

export interface EstimateLineItem {
  code?: string;
  description?: string;
  quantity?: number;
  unit?: string;
  unitCost?: number;
  total?: number;
}

export interface Estimate extends Document {
  projectId: string;
  organizationId: string;
  createdByUserId: string;
  name: string;
  description?: string;
  status: EstimateStatus;
  totalAmount?: number;
  lineItems?: EstimateLineItem[];
  revision?: number;
  notes?: string;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const EstimateSchema = new Schema<Estimate>(
  {
    projectId: { type: String, required: true, index: true },
    organizationId: { type: String, required: true, index: true },
    createdByUserId: { type: String, required: true },
    name: { type: String, required: true },
    description: { type: String },
    status: { type: String, enum: ['draft', 'final', 'archived'], default: 'draft' },
    totalAmount: { type: Number },
    lineItems: [
      {
        code: { type: String },
        description: { type: String },
        quantity: { type: Number },
        unit: { type: String },
        unitCost: { type: Number },
        total: { type: Number },
      },
    ],
    revision: { type: Number, default: 1 },
    notes: { type: String },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

EstimateSchema.index({ projectId: 1, name: 1, archivedAt: 1 });
EstimateSchema.index({ projectId: 1, archivedAt: 1 });

