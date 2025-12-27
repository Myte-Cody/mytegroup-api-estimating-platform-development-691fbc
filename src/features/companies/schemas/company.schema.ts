import { Document, Schema } from 'mongoose';

export interface Company extends Document {
  orgId: string;
  name: string;
  normalizedName: string;
  externalId?: string | null;
  website?: string | null;
  mainEmail?: string | null;
  mainPhone?: string | null;
  companyTypeKeys: string[];
  tagKeys: string[];
  rating?: number | null;
  notes?: string | null;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const CompanySchema = new Schema<Company>(
  {
    orgId: { type: String, required: true, index: true },
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
    website: { type: String, default: null },
    mainEmail: { type: String, default: null },
    mainPhone: { type: String, default: null },
    companyTypeKeys: { type: [String], default: [] },
    tagKeys: { type: [String], default: [] },
    rating: { type: Number, default: null },
    notes: { type: String, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

CompanySchema.index(
  { orgId: 1, normalizedName: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null } }
);
CompanySchema.index(
  { orgId: 1, externalId: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null, externalId: { $ne: null } } }
);
CompanySchema.index({ orgId: 1, archivedAt: 1 });
CompanySchema.index({ orgId: 1, companyTypeKeys: 1 });
CompanySchema.index({ orgId: 1, tagKeys: 1 });

