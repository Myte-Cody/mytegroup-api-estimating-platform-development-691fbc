/**
 * Waitlist module configuration (keep knobs here to avoid polluting .env).
 * Adjust values below to tune marketing display, invite cadence, and gating.
 */
export const waitlistConfig = {
  marketing: {
    /**
     * Display projection will trend toward targetCount over targetDays from campaignStart.
     * baselineCount is the starting point; jitterRange adds small daily drift for return visitors.
     */
    campaignStart: new Date('2024-11-15T00:00:00Z'),
    baselineCount: 118, // starting visible waitlist count
    targetCount: 275, // cap for FOMO story; clamp display at this value
    targetDays: 90, // days to reach targetCount from baselineCount
    jitterRange: 4, // +/- jitter applied per day to avoid static numbers
    overrideDisplayCount: undefined as number | undefined, // set to a number to pin the display
    freeSeatsPerOrg: 5, // marketing copy for free seats per org
  },
  verification: {
    codeLength: 6, // digits in the email verification code
    ttlMinutes: 30, // how long a verification code is valid
    maxAttempts: 5, // max attempts per code before requiring a new one
    resendCooldownMinutes: 2, // minimum minutes between resends to the same email
    maxResendsPerWindow: 3, // cap resends within the rateLimit.windowMs window
    maxTotalAttempts: 12, // total failed attempts across codes before blocking
    maxTotalResends: 6, // total resends before blocking
    blockMinutes: 60, // how long to block verification after exceeding limits
  },
  // Captcha is intentionally omitted for now.
  // We rely on domain policy + email verification codes + Redis rate limiting.
  domainPolicy: {
    // Block disposable and common personal email providers; enforce company domains for waitlist.
    denylist: [
      'mailinator.com',
      '10minutemail.com',
      'tempmail.com',
      'gmail.com',
      'yahoo.com',
      'yahoo.ca',
      'outlook.com',
      'outlook.fr',
      'hotmail.com',
      'live.com',
      'icloud.com',
      'me.com',
      'proton.me',
      'protonmail.com',
      'aol.com',
    ],
  },
  invite: {
    enforceGate: true, // when true, registration requires waitlist status=invited
    delayHours: 36, // minimum hours after join before auto-invite eligibility
    window: { start: '09:00', end: '17:00', timezone: 'America/New_York' }, // invite send window
    batchLimit: 15, // max invites per automated batch
    cohortTag: 'wave-1', // default cohort tag applied on invite
    autoInviteEnabled: true, // run automated invite batches
    intervalMs: 10 * 60 * 1000, // how often the auto-invite worker runs
    domainGateEnabled: true, // when true, only the first registrant per domain can create an org
  },
  rateLimit: {
    windowMs: 15 * 60 * 1000, // 15 minutes
    submissionsPerIp: 8,
    submissionsPerEmail: 3,
    eventsPerIp: 120,
  },
}
