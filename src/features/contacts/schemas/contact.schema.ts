import { Document, Schema } from 'mongoose';

export type PersonType = 'staff' | 'ironworker' | 'external';
export type ContactKind = 'individual' | 'business';

export type ContactCertification = {
  name: string;
  issuedAt?: Date | null;
  expiresAt?: Date | null;
  documentUrl?: string | null;
  notes?: string | null;
};

export interface Contact extends Document {
  orgId: string;
  name: string;
  personType?: PersonType;
  contactKind?: ContactKind;
  firstName?: string | null;
  lastName?: string | null;
  displayName?: string | null;
  dateOfBirth?: Date | null;
  ironworkerNumber?: string | null;
  unionLocal?: string | null;
  promotedToForeman?: boolean;
  foremanUserId?: string | null;
  officeId?: string | null;
  reportsToContactId?: string | null;
  skills?: string[];
  certifications?: ContactCertification[];
  rating?: number | null;
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
    personType: { type: String, enum: ['staff', 'ironworker', 'external'], default: 'external', index: true },
    contactKind: { type: String, enum: ['individual', 'business'], default: 'individual' },
    firstName: { type: String, default: null },
    lastName: { type: String, default: null },
    displayName: { type: String, default: null },
    dateOfBirth: { type: Date, default: null },
    ironworkerNumber: { type: String, default: null, index: true },
    unionLocal: { type: String, default: null },
    promotedToForeman: { type: Boolean, default: false },
    foremanUserId: { type: String, default: null },
    officeId: { type: String, default: null },
    reportsToContactId: { type: String, default: null },
    skills: { type: [String], default: [] },
    certifications: {
      type: [
        {
          name: { type: String, required: true },
          issuedAt: { type: Date, default: null },
          expiresAt: { type: Date, default: null },
          documentUrl: { type: String, default: null },
          notes: { type: String, default: null },
        },
      ],
      default: [],
    },
    rating: { type: Number, default: null },
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
ContactSchema.index({ orgId: 1, personType: 1, archivedAt: 1 });
ContactSchema.index({ orgId: 1, ironworkerNumber: 1 });
ContactSchema.index({ orgId: 1, archivedAt: 1 });
