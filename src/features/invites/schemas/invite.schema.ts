import { Document, Schema } from 'mongoose';
import { Role } from '../../../common/roles';

export type InviteStatus = 'pending' | 'accepted' | 'expired';

export interface Invite extends Document {
  orgId: string;
  email: string;
  role: Role;
  contactId?: string | null;
  tokenHash: string;
  tokenExpires: Date;
  status: InviteStatus;
  createdByUserId: string;
  invitedUserId?: string | null;
  acceptedAt?: Date | null;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const InviteSchema = new Schema<Invite>(
  {
    orgId: { type: String, required: true, index: true },
    email: { type: String, required: true },
    role: { type: String, required: true },
    contactId: { type: String, default: null },
    tokenHash: { type: String, required: true },
    tokenExpires: { type: Date, required: true },
    status: { type: String, enum: ['pending', 'accepted', 'expired'], default: 'pending' },
    createdByUserId: { type: String, required: true },
    invitedUserId: { type: String, default: null },
    acceptedAt: { type: Date, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

InviteSchema.index({ orgId: 1, email: 1, status: 1 });
InviteSchema.index({ tokenHash: 1 });
InviteSchema.index({ tokenExpires: 1 }, { expireAfterSeconds: 0 });
