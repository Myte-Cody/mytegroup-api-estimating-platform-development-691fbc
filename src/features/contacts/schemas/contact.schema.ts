import { Document, Schema } from 'mongoose';

export interface Contact extends Document {
  orgId: string;
  name: string;
  email?: string | null;
  phone?: string | null;
  company?: string | null;
  roles: string[];
  tags: string[];
  notes?: string | null;
  invitedUserId?: string | null;
  invitedAt?: Date | null;
  inviteStatus?: 'pending' | 'accepted' | null;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const ContactSchema = new Schema<Contact>(
  {
    orgId: { type: String, required: true, index: true },
    name: { type: String, required: true },
    email: { type: String, default: null },
    phone: { type: String, default: null },
    company: { type: String, default: null },
    roles: { type: [String], default: [] },
    tags: { type: [String], default: [] },
    notes: { type: String, default: null },
    invitedUserId: { type: String, default: null },
    invitedAt: { type: Date, default: null },
    inviteStatus: { type: String, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

ContactSchema.index({ orgId: 1, email: 1 });
