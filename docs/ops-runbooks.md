# Ops Runbooks (Backend)

Use these runbooks for operations until a formal playbook system is in place.

## 1) Legal Docs (PP/T&C) Update & Enforcement
- Roles: superadmin/platform_admin only.
- Steps:
  1. Publish new version via `POST /legal` (type, version, content, effectiveAt optional).
  2. Set `LEGAL_ENFORCE=1` to require acceptance; ensure legal routes stay accessible.
  3. (Optional) Notify users via email; log `legal.updated` and monitor acceptance events.
- Acceptance: `/legal/:type` returns new version; `/legal/acceptance/status` shows pending; audit logs contain `legal.updated`/`legal.accepted`.

## 2) Compliance/DSR/PII Requests
- Roles: compliance/compliance_officer/security admins.
- Steps:
  1. Identify entity records (users/contacts/projects/offices/etc.).
  2. Use `/compliance/strip-pii` for redaction; `/compliance/legal-hold` to freeze; `/compliance/batch-archive` if required.
  3. Confirm audit events emitted; verify PII is redacted on exports.
- Acceptance: Records show `piiStripped` or `legalHold`; audit logs contain compliance events; exports/audit payloads redacted.

## 3) Tenant Migration (Shared ↔ Dedicated)
- Roles: superadmin only.
- Steps:
  1. Start via `/migration/start` with orgId, direction, chunkSize (optional), targetUri/dbName (if needed).
  2. Monitor `/migration/status/:orgId`; ensure progress updates; dry-run before cutover.
  3. For cutover (shared→dedicated), validate target connection; finalize via `/migration/finalize`.
  4. Abort via `/migration/abort` if needed; rollback cleans target data (best effort).
- Acceptance: Migration status `ready_for_cutover` (or `completed` for dry-run); audit events for start/progress/finalize/abort; legal hold respected unless override specified.

## 4) Incident Response (Security/Rate Limit Abuse)
- Steps:
  1. Identify source (IP/user); check rate-limit logs/429s.
  2. Increase rate-limit strictness or block offending IPs via upstream firewall if needed.
  3. Verify session compromise: rotate `SESSION_SECRET`, invalidate sessions (flush store).
  4. Review audit logs for abnormal activity; place affected orgs/users on `legalHold` if needed.
- Acceptance: Abuse mitigated (no further 429 spikes); sessions rotated; audit shows containment actions.

## 5) Email Delivery Issues
- Steps:
  1. Verify SMTP env (`SMTP_SERVER`, `SMTP_USER/PASS`, `SMTP_FROM`, port/secure).
  2. Ensure `SMTP_TEST_MODE` is off in prod; on in non-prod/CI.
  3. Check allowlist for test sends (`TEST_EMAIL_ALLOWLIST`).
- Acceptance: Emails sent successfully; audit/logs show send attempts; no real sends in test/CI.

## 6) Monitoring & Alerts (Guidance)
- Alert on: error rate spikes, rate-limit spikes, migration failures, legal/compliance events, email send failures.
- Logs should redact PII; event log TTL ~10 years with legal hold exemption.

## 7) Session/Secret Rotation
- Steps:
  1. Set new `SESSION_SECRET` and restart services.
  2. Flush session store to invalidate old sessions (prod store required; do not rely on in-memory).
  3. Rotate SMTP/JWT/DB creds per policy; update env only (never commit).
- Acceptance: Users re-authenticate; no stale sessions; secrets only in env.
