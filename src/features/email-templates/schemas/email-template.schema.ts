import { Document, Schema } from 'mongoose';
import { EMAIL_TEMPLATE_DEFAULT_LOCALE } from '../email-template.constants';

export interface EmailTemplate extends Document {
  orgId: string;
  name: string;
  locale: string;
  subject: string;
  html: string;
  text: string;
  requiredVariables: string[];
  optionalVariables: string[];
  archivedAt?: Date | null;
  piiStripped: boolean;
  legalHold: boolean;
  createdByUserId?: string | null;
  updatedByUserId?: string | null;
  createdAt: Date;
  updatedAt: Date;
}

export const EmailTemplateSchema = new Schema<EmailTemplate>(
  {
    orgId: { type: String, required: true, index: true },
    name: { type: String, required: true },
    locale: { type: String, default: EMAIL_TEMPLATE_DEFAULT_LOCALE },
    subject: { type: String, required: true },
    html: { type: String, required: true },
    text: { type: String, required: true },
    requiredVariables: { type: [String], default: [] },
    optionalVariables: { type: [String], default: [] },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
    createdByUserId: { type: String, default: null },
    updatedByUserId: { type: String, default: null },
  },
  { timestamps: true }
);

EmailTemplateSchema.index({ orgId: 1, name: 1, locale: 1 }, { unique: true });
