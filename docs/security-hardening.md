# Security & Compliance Hardening Checklist (Backend)

Use this as a pre-flight checklist for prod. All secrets must be sourced from env; never commit secrets.

## Sessions & Cookies
- Set `SESSION_SECRET` to a strong secret; rotate periodically.
- Use a persistent session store in prod (Redis/Mongo). Current setup defaults to memory for dev; configure store in deployment/bootstrap.
- Cookie settings:
  - `SESSION_COOKIE_SECURE=true` in prod.
  - `SESSION_COOKIE_SAMESITE=strict` unless cross-site is required (use `none` only when needed and served over HTTPS).
  - `SESSION_COOKIE_DOMAIN` aligned to client domain; `HttpOnly=true` (default).

## Auth & Passwords
- Passwords are bcrypt-hashed; enforce strong password policy (already enabled).
- Keep login endpoints rate-limited (enabled).
- If JWT is introduced, enforce audience/issuer/expiry and key rotation.

## Secrets & Config
- All secrets via env: Mongo, SMTP, session secret, any JWT keys.
- No secrets in code/tests; validate with secret scanners in CI.
- Document rotation cadence for session secret/SMTP creds/JWT keys.

## PII & Compliance
- Entities include `archivedAt`, `piiStripped`, `legalHold`. Ensure new entities follow suit.
- Legal hold blocks destructive updates; PII stripping redacts exports and audit payloads.
- Legal acceptance enforced via `LegalGuard` when `LEGAL_ENFORCE=1`.
- Keep DSR/PII request runbook updated (see `ops-runbooks.md`).

## Rate Limiting
- Auth and bulk endpoints are rate-limited; configure per-IP (default) or per-user key if behind a proxy.
- Monitor 429s for abuse; alert on spikes.

## Email Safety
- `SMTP_TEST_MODE=1` in test/CI to use stub transport.
- Test-send endpoints require allowlist (`TEST_EMAIL_ALLOWLIST`); keep allowlist minimal in prod and non-prod.

## Logging & Audit
- All mutations should emit audit events (`audit.logMutation` helper). Ensure new endpoints are instrumented.
- Event log redacts payload/metadata when `piiStripped` or `legalHold` is true.
- Retention: 10-year TTL; legal hold disables TTL.

## Storage & Uploads
- Configure `STORAGE_PROVIDER` and `ALLOW_MIME_TYPES`; keep max upload size sane (`MAX_UPLOAD_BYTES`).
- Validate MIME types and scan uploads upstream if S3 is used.

## Monitoring & Alerts
- Add alerting for error rate spikes, rate limit abuse, migration failures, and legal/compliance events.
- Track email delivery failures (SMTP) and background job errors (if applicable).
