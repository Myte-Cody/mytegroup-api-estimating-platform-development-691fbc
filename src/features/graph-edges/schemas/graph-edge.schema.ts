import { Document, Schema } from 'mongoose';

export type GraphNodeType = 'person' | 'org_location' | 'company' | 'company_location';

export interface GraphEdge extends Document {
  orgId: string;
  fromNodeType: GraphNodeType;
  fromNodeId: string;
  toNodeType: GraphNodeType;
  toNodeId: string;
  edgeTypeKey: string;
  metadata?: Record<string, any>;
  effectiveFrom?: Date | null;
  effectiveTo?: Date | null;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const GraphEdgeSchema = new Schema<GraphEdge>(
  {
    orgId: { type: String, required: true, index: true },
    fromNodeType: { type: String, required: true, index: true },
    fromNodeId: { type: String, required: true, index: true },
    toNodeType: { type: String, required: true, index: true },
    toNodeId: { type: String, required: true, index: true },
    edgeTypeKey: { type: String, required: true, index: true },
    metadata: { type: Schema.Types.Mixed, default: {} },
    effectiveFrom: { type: Date, default: null },
    effectiveTo: { type: Date, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

GraphEdgeSchema.index({ orgId: 1, fromNodeType: 1, fromNodeId: 1 });
GraphEdgeSchema.index({ orgId: 1, toNodeType: 1, toNodeId: 1 });
GraphEdgeSchema.index(
  { orgId: 1, edgeTypeKey: 1, fromNodeType: 1, fromNodeId: 1, toNodeType: 1, toNodeId: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null } }
);
GraphEdgeSchema.index({ orgId: 1, archivedAt: 1 });

