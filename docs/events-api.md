# Audit/Event Log API

Endpoints are read-only and tenant-scoped. All routes require session auth plus `OrgScopeGuard` and `RolesGuard`.

## Endpoints
- `GET /events`: list events with filters and cursor pagination.
- `GET /events/:id`: fetch a single event by id (scoped to org).

## Roles Allowed
- `admin`, `compliance`, `security`, `superadmin`, `platform_admin`
- `org_owner` is not permitted for audit log access.

## Org Scoping
- Non-superadmin requests are always scoped to the session `orgId`.
- Superadmin must supply `orgId` to read any org; if missing, the request is rejected.

## Filters (`GET /events`)
- `entityType`, `entityId`, `actor`, `action`, `eventType`
- `createdAtGte`, `createdAtLte` (ISO date)
- `archived` (include archived when `true`)
- `cursor` (base64, opaque)
- `limit` (default 25, max 100)
- `sort` (`asc` or `desc`, defaults to `desc`)
- `orgId` (superadmin only)

## Responses
- List: `{ data: EventLog[], total: number, nextCursor?: string }`
- Single: `EventLog`
- Payload/metadata are redacted when `piiStripped` or `legalHold` is true. `redacted: true` is added when redacted.

## EventLog shape (summary)
- `eventType`, `action`, `entityType`, `entityId`, `actor`, `orgId`, `payload`, `metadata`, `archivedAt`, `piiStripped`, `legalHold`, `createdAt`
- TTL: `createdAt` expires after ~10 years; TTL is skipped when `legalHold` is true.
