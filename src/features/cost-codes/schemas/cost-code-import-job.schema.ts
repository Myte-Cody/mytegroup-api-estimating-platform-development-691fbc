import { Document, Schema } from 'mongoose';

export type CostCodeImportStatus = 'queued' | 'processing' | 'preview' | 'done' | 'failed';

export interface CostCodeImportJob extends Document {
  orgId: string;
  status: CostCodeImportStatus;
  preview: Array<{ category: string; code: string; description: string }>;
  errorMessage?: string | null;
  fileName?: string | null;
  fileMime?: string | null;
  fileSize?: number | null;
  fileBase64?: string | null;
  createdAt: Date;
  updatedAt: Date;
}

export const CostCodeImportJobSchema = new Schema<CostCodeImportJob>(
  {
    orgId: { type: String, required: true, index: true },
    status: {
      type: String,
      enum: ['queued', 'processing', 'preview', 'done', 'failed'],
      default: 'queued',
    },
    preview: { type: [Schema.Types.Mixed], default: [] },
    errorMessage: { type: String, default: null },
    fileName: { type: String, default: null },
    fileMime: { type: String, default: null },
    fileSize: { type: Number, default: null },
    fileBase64: { type: String, default: null },
  },
  { timestamps: true }
);
