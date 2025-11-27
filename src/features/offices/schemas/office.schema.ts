import { Schema, Document } from 'mongoose';

export interface Office extends Document {
  name: string;
  organizationId: string;
  address?: string;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const OfficeSchema = new Schema<Office>(
  {
    name: { type: String, required: true },
    organizationId: { type: String, required: true },
    address: { type: String },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

OfficeSchema.index({ organizationId: 1, name: 1 }, { unique: true });
OfficeSchema.index({ organizationId: 1, archivedAt: 1 });
