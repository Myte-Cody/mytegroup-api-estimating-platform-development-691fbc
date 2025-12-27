import { Document, Schema } from 'mongoose';
import { normalizeName } from '../../../common/utils/normalize.util';

export interface CompanyLocation extends Document {
  orgId: string;
  companyId: string;
  name: string;
  normalizedName: string;
  externalId?: string | null;
  timezone?: string | null;
  email?: string | null;
  phone?: string | null;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  region?: string | null;
  postal?: string | null;
  country?: string | null;
  tagKeys: string[];
  notes?: string | null;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const CompanyLocationSchema = new Schema<CompanyLocation>(
  {
    orgId: { type: String, required: true, index: true },
    companyId: { type: String, required: true, index: true },
    name: { type: String, required: true },
    normalizedName: { type: String, required: true, index: true },
    externalId: {
      type: String,
      default: null,
      set: (value: unknown) => {
        if (typeof value !== 'string') return null;
        const trimmed = value.trim();
        return trimmed ? trimmed : null;
      },
    },
    timezone: { type: String, default: null },
    email: { type: String, default: null },
    phone: { type: String, default: null },
    addressLine1: { type: String, default: null },
    addressLine2: { type: String, default: null },
    city: { type: String, default: null },
    region: { type: String, default: null },
    postal: { type: String, default: null },
    country: { type: String, default: null },
    tagKeys: { type: [String], default: [] },
    notes: { type: String, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

CompanyLocationSchema.pre('validate', function (next) {
  const doc = this as any;
  if (!doc.normalizedName && typeof doc.name === 'string') {
    doc.normalizedName = normalizeName(doc.name);
  }
  next();
});

CompanyLocationSchema.index({ orgId: 1, companyId: 1 });
CompanyLocationSchema.index(
  { orgId: 1, companyId: 1, normalizedName: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null } }
);
CompanyLocationSchema.index(
  { orgId: 1, companyId: 1, externalId: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null, externalId: { $ne: null } } }
);
CompanyLocationSchema.index({ orgId: 1, tagKeys: 1 });
CompanyLocationSchema.index({ orgId: 1, companyId: 1, tagKeys: 1 });
CompanyLocationSchema.index({ orgId: 1, archivedAt: 1 });

