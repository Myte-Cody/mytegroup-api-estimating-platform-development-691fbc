import { Schema, Document } from 'mongoose';

export interface Project extends Document {
  name: string;
  orgId: string;
  officeId?: string;
  description?: string;
  projectCode?: string | null;
  status?: string | null;
  location?: string | null;
  bidDate?: Date | null;
  awardDate?: Date | null;
  fabricationStartDate?: Date | null;
  fabricationEndDate?: Date | null;
  erectionStartDate?: Date | null;
  erectionEndDate?: Date | null;
  completionDate?: Date | null;
  budget?: {
    hours?: number | null;
    labourRate?: number | null;
    currency?: string | null;
    amount?: number | null;
  } | null;
  quantities?: {
    structural?: { tonnage?: number | null; pieces?: number | null };
    miscMetals?: { tonnage?: number | null; pieces?: number | null };
    metalDeck?: { pieces?: number | null; sqft?: number | null };
    cltPanels?: { pieces?: number | null; sqft?: number | null };
    glulam?: { volumeM3?: number | null; pieces?: number | null };
  } | null;
  staffing?: {
    projectManagerPersonId?: string | null;
    foremanPersonIds?: string[];
    superintendentPersonId?: string | null;
  } | null;
  costCodeBudgets?: Array<{
    costCodeId: string;
    budgetedHours?: number | null;
    costBudget?: number | null;
  }>;
  seatAssignments?: Array<{
    seatId: string;
    personId?: string | null;
    role?: string | null;
    assignedAt?: Date | null;
    removedAt?: Date | null;
  }>;
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
    projectCode: { type: String, default: null },
    status: { type: String, default: null },
    location: { type: String, default: null },
    bidDate: { type: Date, default: null },
    awardDate: { type: Date, default: null },
    fabricationStartDate: { type: Date, default: null },
    fabricationEndDate: { type: Date, default: null },
    erectionStartDate: { type: Date, default: null },
    erectionEndDate: { type: Date, default: null },
    completionDate: { type: Date, default: null },
    budget: {
      type: {
        hours: { type: Number, default: null },
        labourRate: { type: Number, default: null },
        currency: { type: String, default: null },
        amount: { type: Number, default: null },
      },
      default: null,
    },
    quantities: {
      type: {
        structural: {
          type: {
            tonnage: { type: Number, default: null },
            pieces: { type: Number, default: null },
          },
          default: {},
        },
        miscMetals: {
          type: {
            tonnage: { type: Number, default: null },
            pieces: { type: Number, default: null },
          },
          default: {},
        },
        metalDeck: {
          type: {
            pieces: { type: Number, default: null },
            sqft: { type: Number, default: null },
          },
          default: {},
        },
        cltPanels: {
          type: {
            pieces: { type: Number, default: null },
            sqft: { type: Number, default: null },
          },
          default: {},
        },
        glulam: {
          type: {
            volumeM3: { type: Number, default: null },
            pieces: { type: Number, default: null },
          },
          default: {},
        },
      },
      default: null,
    },
    staffing: {
      type: {
        projectManagerPersonId: { type: String, default: null },
        foremanPersonIds: { type: [String], default: [] },
        superintendentPersonId: { type: String, default: null },
      },
      default: null,
    },
    costCodeBudgets: {
      type: [
        {
          costCodeId: { type: String, required: true },
          budgetedHours: { type: Number, default: null },
          costBudget: { type: Number, default: null },
        },
      ],
      default: [],
    },
    seatAssignments: {
      type: [
        {
          seatId: { type: String, required: true },
          personId: { type: String, default: null },
          role: { type: String, default: null },
          assignedAt: { type: Date, default: null },
          removedAt: { type: Date, default: null },
        },
      ],
      default: [],
    },
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
ProjectSchema.index(
  { orgId: 1, projectCode: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null, projectCode: { $type: 'string' } } }
);
ProjectSchema.index({ orgId: 1, archivedAt: 1 });
