import { Document, Schema } from 'mongoose';

export type MigrationDirection = 'shared_to_dedicated' | 'dedicated_to_shared';
export type MigrationStatus =
  | 'pending'
  | 'in_progress'
  | 'ready_for_cutover'
  | 'completed'
  | 'failed'
  | 'aborted';

export interface CollectionProgress {
  total: number;
  copied: number;
  lastId?: string | null;
}

export interface TenantMigration extends Document {
  orgId: string;
  direction: MigrationDirection;
  status: MigrationStatus;
  dryRun: boolean;
  resumeRequested: boolean;
  allowLegalHoldOverride: boolean;
  actorUserId?: string;
  actorRole?: string;
  targetUri?: string | null;
  targetDbName?: string | null;
  chunkSize: number;
  progress: Record<string, CollectionProgress>;
  error?: string | null;
  startedAt: Date;
  completedAt?: Date | null;
  lastProgressAt?: Date | null;
  metadata?: Record<string, unknown>;
}

export const TenantMigrationSchema = new Schema<TenantMigration>(
  {
    orgId: { type: String, required: true, index: true },
    direction: { type: String, required: true },
    status: { type: String, required: true, default: 'pending' },
    dryRun: { type: Boolean, default: false },
    resumeRequested: { type: Boolean, default: true },
    allowLegalHoldOverride: { type: Boolean, default: false },
    actorUserId: { type: String },
    actorRole: { type: String },
    targetUri: { type: String, default: null },
    targetDbName: { type: String, default: null },
    chunkSize: { type: Number, default: 100 },
    progress: { type: Schema.Types.Mixed, default: {} },
    error: { type: String, default: null },
    startedAt: { type: Date, default: Date.now },
    completedAt: { type: Date, default: null },
    lastProgressAt: { type: Date, default: null },
    metadata: { type: Schema.Types.Mixed, default: {} },
  },
  { timestamps: true }
);
