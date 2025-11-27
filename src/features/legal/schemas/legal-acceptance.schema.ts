import { Schema, Document } from 'mongoose';
import { LegalDocType } from '../legal.types';

export interface LegalAcceptance extends Document {
  userId: string;
  orgId?: string;
  docType: LegalDocType;
  version: string;
  acceptedAt: Date;
  ipAddress?: string;
  userAgent?: string;
  archivedAt: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
}

export const LegalAcceptanceSchema = new Schema<LegalAcceptance>(
  {
    userId: { type: String, required: true, index: true },
    orgId: { type: String, index: true },
    docType: { type: String, enum: Object.values(LegalDocType), required: true },
    version: { type: String, required: true },
    acceptedAt: { type: Date, default: Date.now },
    ipAddress: { type: String },
    userAgent: { type: String },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { versionKey: false, timestamps: { createdAt: true, updatedAt: false } }
);

LegalAcceptanceSchema.index({ userId: 1, docType: 1, version: 1 }, { unique: true });
LegalAcceptanceSchema.index({ orgId: 1, docType: 1, version: 1 });
