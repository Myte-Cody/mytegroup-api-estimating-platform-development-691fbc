import { Schema, Document } from 'mongoose';

export interface Organization extends Document {
  name: string;
  metadata?: Record<string, unknown>;
  ownerUserId?: string | null;
  createdByUserId?: string | null;
  primaryDomain?: string | null;
  useDedicatedDb: boolean;
  datastoreType?: 'shared' | 'dedicated';
  databaseUri?: string | null;
  databaseName?: string | null;
  dataResidency?: 'shared' | 'dedicated';
  lastMigratedAt?: Date | null;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  datastoreHistory?: {
    fromType?: string;
    toType?: string;
    fromUri?: string | null;
    toUri?: string | null;
    actorId?: string | null;
    switchedAt: Date;
  }[];
  createdAt: Date;
  updatedAt: Date;
}

export const OrganizationSchema = new Schema<Organization>(
  {
    name: { type: String, required: true, unique: true },
    metadata: { type: Schema.Types.Mixed, default: {} },
    ownerUserId: { type: String, default: null },
    createdByUserId: { type: String, default: null },
    primaryDomain: {
      type: String,
      index: true,
      unique: true,
      sparse: true,
      set: (value: unknown) => {
        if (typeof value !== 'string') return undefined;
        const normalized = value.trim().toLowerCase();
        return normalized ? normalized : undefined;
      },
    },
    useDedicatedDb: { type: Boolean, default: false },
    datastoreType: { type: String, enum: ['shared', 'dedicated'], default: 'shared' },
    databaseUri: { type: String, default: null },
    databaseName: { type: String, default: null },
    dataResidency: { type: String, enum: ['shared', 'dedicated'], default: 'shared' },
    lastMigratedAt: { type: Date, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
    datastoreHistory: [
      {
        fromType: { type: String },
        toType: { type: String },
        fromUri: { type: String, default: null },
        toUri: { type: String, default: null },
        actorId: { type: String, default: null },
        switchedAt: { type: Date, default: Date.now },
      },
    ],
  },
  { timestamps: true }
);
