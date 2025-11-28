import { Schema, Document } from 'mongoose';

export interface User extends Document {
  firstName?: string;
  lastName?: string;
  username: string;
  email: string;
  passwordHash: string;
  role: string;
  roles: string[];
  organizationId?: string;
  isEmailVerified: boolean;
  verificationTokenHash?: string | null;
  verificationTokenExpires?: Date | null;
  resetTokenHash?: string | null;
  resetTokenExpires?: Date | null;
  archivedAt?: Date | null;
  lastLogin?: Date | null;
  isOrgOwner: boolean;
  piiStripped: boolean;
  legalHold: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const UserSchema = new Schema<User>(
  {
    firstName: { type: String, default: null },
    lastName: { type: String, default: null },
    username: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    passwordHash: { type: String, required: true },
    role: { type: String, default: 'user' },
    roles: { type: [String], default: ['user'] },
    organizationId: { type: String },
    isEmailVerified: { type: Boolean, default: false },
    verificationTokenHash: { type: String, default: null },
    verificationTokenExpires: { type: Date, default: null },
    resetTokenHash: { type: String, default: null },
    resetTokenExpires: { type: Date, default: null },
    archivedAt: { type: Date, default: null },
    lastLogin: { type: Date, default: null },
    isOrgOwner: { type: Boolean, default: false },
    piiStripped: { type: Boolean, default: false },
    legalHold: { type: Boolean, default: false },
  },
  { timestamps: true }
);

UserSchema.index({ organizationId: 1, archivedAt: 1 });
