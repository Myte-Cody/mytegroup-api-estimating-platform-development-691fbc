import { Schema, Document } from 'mongoose';
import { LegalDocType } from '../legal.types';

export interface LegalDoc extends Document {
  type: LegalDocType;
  version: string;
  content: string;
  effectiveAt: Date;
  archivedAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
}

export const LegalDocSchema = new Schema<LegalDoc>(
  {
    type: { type: String, enum: Object.values(LegalDocType), required: true },
    version: { type: String, required: true },
    content: { type: String, required: true },
    effectiveAt: { type: Date, default: Date.now },
    archivedAt: { type: Date, default: null },
  },
  { versionKey: false, timestamps: { createdAt: true, updatedAt: true } }
);

LegalDocSchema.index({ type: 1, version: 1 }, { unique: true });
LegalDocSchema.index({ type: 1, effectiveAt: -1, createdAt: -1 });
