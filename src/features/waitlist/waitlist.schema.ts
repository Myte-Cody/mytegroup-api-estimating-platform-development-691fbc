import { Document, Schema } from 'mongoose'

export type WaitlistStatus = 'pending-cohort' | 'invited' | 'activated'
export type WaitlistVerifyStatus = 'unverified' | 'verified' | 'blocked'

export interface WaitlistEntry extends Document {
  email: string
  name: string
  role: string
  source?: string | null
  status: WaitlistStatus
  verifyStatus: WaitlistVerifyStatus
  verifyCode?: string | null
  verifyExpiresAt?: Date | null
  verifyAttempts: number
  verifyAttemptTotal: number
  verifyResends: number
  lastVerifySentAt?: Date | null
  verifiedAt?: Date | null
  verifyBlockedAt?: Date | null
  verifyBlockedUntil?: Date | null
  preCreateAccount: boolean
  marketingConsent: boolean
  invitedAt?: Date | null
  activatedAt?: Date | null
  cohortTag?: string | null
  metadata?: Record<string, any> | null
  inviteFailureCount?: number
  createdAt: Date
  updatedAt: Date
  archivedAt?: Date | null
  piiStripped: boolean
  legalHold: boolean
}

export const WaitlistSchema = new Schema<WaitlistEntry>(
  {
    email: { type: String, required: true, unique: true, index: true },
    name: { type: String, required: true },
    role: { type: String, required: true },
    source: { type: String, default: null },
    status: { type: String, default: 'pending-cohort' },
    verifyStatus: { type: String, default: 'unverified' },
    verifyCode: { type: String, default: null },
    verifyExpiresAt: { type: Date, default: null },
    verifyAttempts: { type: Number, default: 0 },
    verifyAttemptTotal: { type: Number, default: 0 },
    verifyResends: { type: Number, default: 0 },
    lastVerifySentAt: { type: Date, default: null },
    verifiedAt: { type: Date, default: null },
    verifyBlockedAt: { type: Date, default: null },
    verifyBlockedUntil: { type: Date, default: null },
    preCreateAccount: { type: Boolean, default: false },
    marketingConsent: { type: Boolean, default: false },
    invitedAt: { type: Date, default: null },
    activatedAt: { type: Date, default: null },
    cohortTag: { type: String, default: null },
    metadata: { type: Object, default: null },
    inviteFailureCount: { type: Number, default: 0 },
    archivedAt: { type: Date, default: null },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
)

WaitlistSchema.index({ status: 1, createdAt: -1 })
WaitlistSchema.index({ verifyStatus: 1, status: 1, createdAt: 1 })
