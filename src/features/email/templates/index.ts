import { buildClientUrl } from '../../../config/app.config'
import { waitlistConfig } from '../../waitlist/waitlist.config'

export const brand = {
  name: 'MYTE Construction OS',
  company: 'Myte Group',
  address: 'Myte Group, 7501 Av. M B Jodoin, Anjou, QC H1J 2H9',
  contact: 'info@mytegroup.com',
  logoLight: buildClientUrl('/LogoFooterForWhiteBG.svg'),
  logoDark: buildClientUrl('/LogoFooterForBlackBG.png'),
}

type TemplateArgs = {
  title: string
  bodyHtml: string
  bodyText: string
  ctaLabel?: string
  ctaHref?: string
  footerNote?: string
  subject: string
}

export const renderBrandedTemplate = ({ title, bodyHtml, ctaLabel, ctaHref, footerNote }: Omit<TemplateArgs, 'subject' | 'bodyText'>) => {
  const ctaBlock =
    ctaLabel && ctaHref
      ? `<p style="margin:20px 0;"><a href="${ctaHref}" style="background:#10b981;color:#0b1727;padding:12px 18px;border-radius:10px;text-decoration:none;font-weight:600;display:inline-block;">${ctaLabel}</a></p>`
      : ''
  return `
  <div style="font-family:Inter,Arial,sans-serif;max-width:640px;margin:auto;padding:28px;background:#0f172a;color:#e2e8f0;border-radius:16px;">
    <div style="text-align:center;margin-bottom:18px;">
      <img src="${brand.logoLight}" alt="${brand.name}" style="height:42px;" />
    </div>
    <h2 style="margin:0 0 12px;text-align:center;">${title}</h2>
    <div style="margin:0 0 12px;line-height:1.6;text-align:center;">${bodyHtml}</div>
    ${ctaBlock}
    ${footerNote ? `<p style="margin:12px 0;color:#cbd5e1;font-size:14px;text-align:center;">${footerNote}</p>` : ''}
    <hr style="border:none;border-top:1px solid #1f2937;margin:20px 0;" />
    <p style="font-size:12px;color:#94a3b8;margin:0;text-align:center;">${brand.company}</p>
    <p style="font-size:12px;color:#94a3b8;margin:0;text-align:center;">${brand.address}</p>
    <p style="font-size:12px;color:#94a3b8;margin:0;text-align:center;">${brand.contact}</p>
  </div>`
}

export const waitlistInviteTemplate = (opts: { registerLink: string; domain: string; calendly: string }) => {
  const subject = "You're off the waitlist-claim your org"
  const bodyText = [
    `You're in. Use this link to claim ${opts.domain} and finish onboarding: ${opts.registerLink}`,
    '',
    'First signup for your domain becomes the org admin. We release cohorts during business hours; you can still start anytime.',
    '',
    `Prefer a live walkthrough? Book a build session: ${opts.calendly}`,
  ].join('\n')
  const bodyHtml = `<p>You're in. First signup for <strong>${opts.domain}</strong> becomes the org admin.</p><p>Finish onboarding and claim ${opts.domain} before this wave closes:</p>`
  const html = renderBrandedTemplate({
    title: "You're off the waitlist—claim your org",
    bodyHtml,
    ctaLabel: 'Finish onboarding',
    ctaHref: opts.registerLink,
    footerNote: `Need help? Book a build session: ${opts.calendly}`,
  })
  return { subject, text: bodyText, html }
}

export const waitlistVerificationTemplate = (opts: { code: string; userName?: string }) => {
  const subject = 'Confirm your email to lock your spot'
  const ttl = waitlistConfig.verification.ttlMinutes || 30
  const bodyText = [
    `Hi${opts.userName ? ' ' + opts.userName : ''},`,
    'Enter this code to confirm your email and hold your place in the next cohort:',
    '',
    `Code: ${opts.code}`,
    '',
    `This code expires in ${ttl} minutes. If you did not request this, you can ignore it.`,
  ].join('\n')

  const codeBlock = `<div style="font-size:26px;font-weight:700;letter-spacing:6px;background:#111827;border:1px solid #1f2937;border-radius:12px;padding:14px 18px;display:inline-block;margin:12px 0;">${opts.code}</div>`
  const bodyHtml = `<p>Hi${opts.userName ? ' ' + opts.userName : ''},</p><p>Enter this code to confirm your email and hold your place in the next cohort.</p>${codeBlock}<p style="color:#cbd5e1;">This code expires in ${ttl} minutes.</p>`

  const html = renderBrandedTemplate({
    title: 'Confirm your email to lock your spot',
    bodyHtml,
    footerNote: 'If you did not request this, you can ignore it.',
  })
  return { subject, text: bodyText, html }
}

export const verificationTemplate = (opts: { verifyLink: string; userName?: string }) => {
  const subject = 'Verify your email'
  const bodyText = `Welcome${opts.userName ? ' ' + opts.userName : ''}! Lock in your access and keep your spot: ${opts.verifyLink}`
  const bodyHtml = `<p>Welcome${opts.userName ? ' ' + opts.userName : ''}!</p><p>Lock in your access and keep your spot in this wave.</p>`
  const html = renderBrandedTemplate({
    title: 'Verify your email',
    bodyHtml,
    ctaLabel: 'Verify email',
    ctaHref: opts.verifyLink,
    footerNote: 'This keeps your place and unlocks your workspace.',
  })
  return { subject, text: bodyText, html }
}

export const passwordResetTemplate = (opts: { resetLink: string; userName?: string }) => {
  const subject = 'Reset your password'
  const bodyText = `We received a request to reset your password.\nReset here: ${opts.resetLink}\nIf you did not request this, you can ignore this email.`
  const bodyHtml = `<p>We received a request to reset your password.</p><p>If you did not request this, you can ignore this email.</p>`
  const html = renderBrandedTemplate({
    title: 'Reset your password',
    bodyHtml,
    ctaLabel: 'Reset password',
    ctaHref: opts.resetLink,
    footerNote: 'If you did not request this, you can ignore this email.',
  })
  return { subject, text: bodyText, html }
}

export const inviteTemplate = (opts: { inviteLink: string; orgName?: string; userName?: string }) => {
  const subject = 'You are invited to Myte Construction'
  const bodyText = [
    `Hello${opts.userName ? ' ' + opts.userName : ''},`,
    `You've been invited to join ${opts.orgName || 'MYTE Construction'}.`,
    `This wave is open—accept your invitation: ${opts.inviteLink}`,
    `If you were not expecting this, you can ignore this email.`,
  ].join('\n')
  const bodyHtml = `<p>Hello${opts.userName ? ' ' + opts.userName : ''},</p><p>You've been invited to join ${opts.orgName || 'MYTE Construction'}.</p><p>This wave is open now.</p>`
  const html = renderBrandedTemplate({
    title: 'You are invited',
    bodyHtml,
    ctaLabel: 'Accept invite',
    ctaHref: opts.inviteLink,
    footerNote: 'If you were not expecting this, you can ignore this email.',
  })
  return { subject, text: bodyText, html }
}
