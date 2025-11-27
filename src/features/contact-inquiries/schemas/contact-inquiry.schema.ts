import { Document, Schema } from 'mongoose'

export type ContactInquiryStatus = 'new' | 'in-progress' | 'closed'

export interface ContactInquiry extends Document {
  name: string
  email: string
  message: string
  source?: string | null
  status: ContactInquiryStatus
  ip?: string | null
  userAgent?: string | null
  respondedAt?: Date | null
  respondedBy?: string | null
  archivedAt?: Date | null
  piiStripped: boolean
  legalHold: boolean
  createdAt: Date
  updatedAt: Date
}

export const ContactInquirySchema = new Schema<ContactInquiry>(
  {
    name: { type: String, required: true },
    email: { type: String, required: true },
    message: { type: String, required: true },
    source: { type: String, default: null },
    status: { type: String, default: 'new' },
    ip: { type: String, default: null },
    userAgent: { type: String, default: null },
    respondedAt: { type: Date, default: null },
    respondedBy: { type: String, default: null },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
)

ContactInquirySchema.index({ createdAt: -1 })
ContactInquirySchema.index({ status: 1, createdAt: -1 })

