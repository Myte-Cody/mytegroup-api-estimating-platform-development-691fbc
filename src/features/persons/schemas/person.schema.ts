import { Document, Schema } from 'mongoose';
import { normalizeEmail, normalizePhoneE164 } from '../../../common/utils/normalize.util';

export type PersonType = 'internal_staff' | 'internal_union' | 'external_person';

export type PersonEmail = {
  value: string;
  normalized: string;
  label?: string | null;
  isPrimary?: boolean;
  verifiedAt?: Date | null;
};

export type PersonPhone = {
  value: string;
  e164: string;
  label?: string | null;
  isPrimary?: boolean;
};

export interface Person extends Document {
  orgId: string;
  externalId?: string | null;
  personType: PersonType;
  firstName?: string | null;
  lastName?: string | null;
  displayName: string;
  dateOfBirth?: Date | null;
  emails: PersonEmail[];
  phones: PersonPhone[];
  primaryEmail?: string | null;
  primaryPhoneE164?: string | null;
  tagKeys: string[];
  skillKeys: string[];
  departmentKey?: string | null;
  orgLocationId?: string | null;
  reportsToPersonId?: string | null;
  ironworkerNumber?: string | null;
  unionLocal?: string | null;
  skillFreeText: string[];
  certifications: Array<{
    name: string;
    issuedAt?: Date | null;
    expiresAt?: Date | null;
    documentUrl?: string | null;
    notes?: string | null;
  }>;
  rating?: number | null;
  notes?: string | null;
  companyId?: string | null;
  companyLocationId?: string | null;
  title?: string | null;
  userId?: string | null;
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

const EmailSchema = new Schema<PersonEmail>(
  {
    value: { type: String, required: true },
    normalized: { type: String, required: true, index: true },
    label: { type: String, default: null },
    isPrimary: { type: Boolean, default: false },
    verifiedAt: { type: Date, default: null },
  },
  { _id: false }
);

const PhoneSchema = new Schema<PersonPhone>(
  {
    value: { type: String, required: true },
    e164: { type: String, required: true, index: true },
    label: { type: String, default: null },
    isPrimary: { type: Boolean, default: false },
  },
  { _id: false }
);

export const PersonSchema = new Schema<Person>(
  {
    orgId: { type: String, required: true, index: true },
    externalId: {
      type: String,
      default: null,
      set: (value: unknown) => {
        if (typeof value !== 'string') return null;
        const trimmed = value.trim();
        return trimmed ? trimmed : null;
      },
    },
    personType: {
      type: String,
      required: true,
      enum: ['internal_staff', 'internal_union', 'external_person'],
      index: true,
    },
    firstName: { type: String, default: null },
    lastName: { type: String, default: null },
    displayName: { type: String, required: true, index: true },
    dateOfBirth: { type: Date, default: null },
    emails: { type: [EmailSchema], default: [] },
    phones: { type: [PhoneSchema], default: [] },
    primaryEmail: { type: String, default: null, index: true },
    primaryPhoneE164: { type: String, default: null, index: true },
    tagKeys: { type: [String], default: [] },
    skillKeys: { type: [String], default: [] },
    departmentKey: { type: String, default: null },
    orgLocationId: { type: String, default: null, index: true },
    reportsToPersonId: { type: String, default: null, index: true },
    ironworkerNumber: { type: String, default: null, index: true },
    unionLocal: { type: String, default: null },
    skillFreeText: { type: [String], default: [] },
    certifications: {
      type: [
        new Schema(
          {
            name: { type: String, required: true },
            issuedAt: { type: Date, default: null },
            expiresAt: { type: Date, default: null },
            documentUrl: { type: String, default: null },
            notes: { type: String, default: null },
          },
          { _id: false }
        ),
      ],
      default: [],
    },
    rating: { type: Number, default: null },
    notes: { type: String, default: null },
    companyId: { type: String, default: null, index: true },
    companyLocationId: { type: String, default: null, index: true },
    title: { type: String, default: null },
    userId: { type: String, default: null, index: true },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

PersonSchema.pre('validate', function (next) {
  const doc = this as any;

  const emails: any[] = Array.isArray(doc.emails) ? doc.emails : [];
  emails.forEach((email) => {
    if (!email) return;
    if (typeof email.value === 'string' && !email.normalized) {
      email.normalized = normalizeEmail(email.value);
    }
  });
  const normalizedEmails = emails.filter((e) => e?.normalized);
  if (normalizedEmails.length) {
    const primaries = normalizedEmails.filter((e) => !!e.isPrimary);
    if (primaries.length !== 1) {
      normalizedEmails.forEach((e, idx) => {
        e.isPrimary = idx === 0;
      });
    }
    const primary = normalizedEmails.find((e) => !!e.isPrimary) || normalizedEmails[0];
    doc.primaryEmail = primary?.normalized || null;
  } else {
    doc.primaryEmail = null;
  }

  const phones: any[] = Array.isArray(doc.phones) ? doc.phones : [];
  phones.forEach((phone) => {
    if (!phone) return;
    if (typeof phone.value === 'string' && !phone.e164) {
      const e164 = normalizePhoneE164(phone.value);
      if (e164) phone.e164 = e164;
    }
  });
  const normalizedPhones = phones.filter((p) => p?.e164);
  if (normalizedPhones.length) {
    const primaries = normalizedPhones.filter((p) => !!p.isPrimary);
    if (primaries.length !== 1) {
      normalizedPhones.forEach((p, idx) => {
        p.isPrimary = idx === 0;
      });
    }
    const primary = normalizedPhones.find((p) => !!p.isPrimary) || normalizedPhones[0];
    doc.primaryPhoneE164 = primary?.e164 || null;
  } else {
    doc.primaryPhoneE164 = null;
  }

  next();
});

PersonSchema.index(
  { orgId: 1, primaryEmail: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null, primaryEmail: { $ne: null } } }
);
PersonSchema.index(
  { orgId: 1, externalId: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null, externalId: { $ne: null } } }
);
PersonSchema.index(
  { orgId: 1, primaryPhoneE164: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null, primaryPhoneE164: { $ne: null } } }
);
PersonSchema.index(
  { orgId: 1, ironworkerNumber: 1 },
  { unique: true, partialFilterExpression: { archivedAt: null, ironworkerNumber: { $ne: null } } }
);
PersonSchema.index({ orgId: 1, 'emails.normalized': 1 });
PersonSchema.index({ orgId: 1, 'phones.e164': 1 });
PersonSchema.index({ orgId: 1, archivedAt: 1 });
