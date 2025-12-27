import { Document, Schema } from 'mongoose';

export type OrgTaxonomyValue = {
  key: string;
  label: string;
  sortOrder?: number | null;
  color?: string | null;
  metadata?: Record<string, any> | null;
  archivedAt?: Date | null;
};

export interface OrgTaxonomy extends Document {
  orgId: string;
  namespace: string;
  values: OrgTaxonomyValue[];
  createdAt: Date;
  updatedAt: Date;
}

const OrgTaxonomyValueSchema = new Schema<OrgTaxonomyValue>(
  {
    key: { type: String, required: true },
    label: { type: String, required: true },
    sortOrder: { type: Number, default: null },
    color: { type: String, default: null },
    metadata: { type: Schema.Types.Mixed, default: {} },
    archivedAt: { type: Date, default: null },
  },
  { _id: false }
);

export const OrgTaxonomySchema = new Schema<OrgTaxonomy>(
  {
    orgId: { type: String, required: true, index: true },
    namespace: { type: String, required: true, index: true },
    values: { type: [OrgTaxonomyValueSchema], default: [] },
  },
  { timestamps: true }
);

OrgTaxonomySchema.index({ orgId: 1, namespace: 1 }, { unique: true });

