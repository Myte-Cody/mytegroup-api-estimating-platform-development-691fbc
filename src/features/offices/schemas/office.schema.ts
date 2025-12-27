import { Schema, Document } from 'mongoose';
import { normalizeName } from '../../../common/utils/normalize.util';

export interface Office extends Document {
  name: string;
  normalizedName: string;
  orgId: string;
  description?: string | null;
  timezone?: string | null;
  orgLocationTypeKey?: string | null;
  tagKeys: string[];
  parentOrgLocationId?: string | null;
  sortOrder?: number | null;
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
    normalizedName: { type: String, required: true, index: true },
    orgId: { type: String, required: true, index: true },
    description: { type: String, default: null },
    timezone: { type: String, default: null },
    orgLocationTypeKey: { type: String, default: null },
    tagKeys: { type: [String], default: [] },
    parentOrgLocationId: { type: String, default: null, index: true },
    sortOrder: { type: Number, default: null },
    address: { type: String },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

OfficeSchema.pre('validate', function (next) {
  const doc = this as any;
  if (!doc.normalizedName && typeof doc.name === 'string') {
    doc.normalizedName = normalizeName(doc.name);
  }
  next();
});

OfficeSchema.index(
  { orgId: 1, normalizedName: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null } }
);
OfficeSchema.index({ orgId: 1, archivedAt: 1 });
OfficeSchema.index({ orgId: 1, parentOrgLocationId: 1 });
