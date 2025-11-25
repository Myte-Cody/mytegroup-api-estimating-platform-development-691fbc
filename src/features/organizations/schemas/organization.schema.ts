import { Schema, Document } from 'mongoose';

export interface Organization extends Document {
  name: string;
  ownerUserId?: string | null;
  createdByUserId?: string | null;
  useDedicatedDb: boolean;
  databaseUri?: string | null;
  databaseName?: string | null;
  dataResidency?: 'shared' | 'dedicated';
  lastMigratedAt?: Date | null;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const OrganizationSchema = new Schema<Organization>(
  {
    name: { type: String, required: true, unique: true },
    ownerUserId: { type: String, default: null },
    createdByUserId: { type: String, default: null },
    useDedicatedDb: { type: Boolean, default: false },
    databaseUri: { type: String, default: null },
    databaseName: { type: String, default: null },
    dataResidency: { type: String, enum: ['shared', 'dedicated'], default: 'shared' },
    lastMigratedAt: { type: Date, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);
