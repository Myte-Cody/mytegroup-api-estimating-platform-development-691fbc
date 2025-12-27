import { Schema, Document } from 'mongoose';

export interface Project extends Document {
  name: string;
  orgId: string;
  officeId?: string;
  description?: string;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const ProjectSchema = new Schema<Project>(
  {
    name: { type: String, required: true },
    orgId: { type: String, required: true, index: true },
    officeId: { type: String },
    description: { type: String },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

ProjectSchema.index(
  { orgId: 1, name: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null } }
);
ProjectSchema.index({ orgId: 1, archivedAt: 1 });
