export type EmailTemplateName = 'invite' | 'verification' | 'password_reset';

export interface EmailTemplateDefinition {
  name: EmailTemplateName;
  subject: string;
  html: string;
  text: string;
  requiredVariables: string[];
  optionalVariables?: string[];
  locale?: string;
  description?: string;
}

export const EMAIL_TEMPLATE_DEFAULT_LOCALE = 'en';

export const EMAIL_TEMPLATE_VARIABLES: Record<EmailTemplateName, string[]> = {
  invite: ['inviteLink'],
  verification: ['verifyLink'],
  password_reset: ['resetLink'],
};

export const BUILTIN_EMAIL_TEMPLATES: EmailTemplateDefinition[] = [
  {
    name: 'invite',
    subject: 'You are invited to Myte Construction',
    html: `<p>Hello {{userName}},</p>
<p>You have been invited to join Myte Construction.</p>
<p>Organization: {{orgName}}</p>
<p>Please accept your invitation here: <a href="{{inviteLink}}">{{inviteLink}}</a></p>
<p>If you were not expecting this email, you can safely ignore it.</p>`,
    text: `Hello {{userName}},

You have been invited to join Myte Construction.
Organization: {{orgName}}
Accept your invitation: {{inviteLink}}

If you were not expecting this email, you can ignore it.`,
    requiredVariables: EMAIL_TEMPLATE_VARIABLES.invite,
    optionalVariables: ['userName', 'orgName', 'senderName'],
    description: 'Invite a new teammate to join an organization.',
    locale: EMAIL_TEMPLATE_DEFAULT_LOCALE,
  },
  {
    name: 'verification',
    subject: 'Verify your email',
    html: `<p>Welcome {{userName}}!</p>
<p>Please verify your email to activate your account.</p>
<p><a href="{{verifyLink}}">Verify your email</a></p>
<p>If you did not sign up for Myte Construction, you can ignore this email.</p>`,
    text: `Welcome {{userName}}!

Please verify your email: {{verifyLink}}

If you did not sign up for Myte Construction, you can ignore this email.`,
    requiredVariables: EMAIL_TEMPLATE_VARIABLES.verification,
    optionalVariables: ['userName'],
    description: 'Email verification for new accounts.',
    locale: EMAIL_TEMPLATE_DEFAULT_LOCALE,
  },
  {
    name: 'password_reset',
    subject: 'Reset your password',
    html: `<p>We received a request to reset your password.</p>
<p>Reset your password here: <a href="{{resetLink}}">{{resetLink}}</a></p>
<p>If you did not request this, you can ignore this email.</p>`,
    text:
      'We received a request to reset your password.\nReset your password: {{resetLink}}\n\nIf you did not request this, you can ignore this email.',
    requiredVariables: EMAIL_TEMPLATE_VARIABLES.password_reset,
    optionalVariables: ['userName'],
    description: 'Password reset instructions.',
    locale: EMAIL_TEMPLATE_DEFAULT_LOCALE,
  },
];
