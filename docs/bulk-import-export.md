# Bulk Import/Export API

## Endpoints

- `POST /bulk-import/:entityType`
  - Accepts CSV or JSON (multipart `file` or JSON `records`).
  - Query params: `dryRun=true|false`, optional `orgId` (SuperAdmin/PlatformAdmin only).
- `GET /bulk-export/:entityType`
  - Query params: `format=csv|json`, `includeArchived=true|false`, optional `orgId` (SuperAdmin/PlatformAdmin only).

Supported entities: `users`, `contacts`, `projects`, `offices`.

## Allowed Columns

- **users**: `username`, `email`, `password`, `role`, `roles`, `isEmailVerified`, `isOrgOwner`, `archivedAt`, `piiStripped`, `legalHold`
- **contacts**: `name`, `email`, `phone`, `company`, `roles`, `tags`, `notes`, `archivedAt`, `piiStripped`, `legalHold`
- **projects**: `name`, `description`, `officeId`, `archivedAt`, `piiStripped`, `legalHold`
- **offices**: `name`, `address`, `archivedAt`, `piiStripped`, `legalHold`

Unknown columns are rejected per row.

## RBAC and Org Scoping

- Allowed roles: SuperAdmin, PlatformAdmin, Admin, OrgOwner, OrgAdmin, Compliance, ComplianceOfficer.
- SuperAdmin/PlatformAdmin can target another org **only when** `orgId` is provided; otherwise the session org is used.
- Other roles are always scoped to the session org; cross-org ids are ignored/rejected.

## Sample Requests

- Import (CSV, dry-run) – arrays accept comma or semicolon separators (e.g., `roles` or `tags`):
  ```bash
  curl -X POST "https://api.example.com/bulk-import/contacts?dryRun=true" \
    -H "Cookie: session=..." \
    -F "file=@contacts.csv"
  ```
  `contacts.csv`:
  ```
  name,email,phone,roles,tags,archivedAt,piiStripped,legalHold
  Alice,alice@example.com,555-1111,manager;viewer,priority;west,,false,false
  Bob,bob@example.com,555-2222,pm, ,2024-05-01T00:00:00Z,true,true
  ```

- Import (JSON):
  ```bash
  curl -X POST "https://api.example.com/bulk-import/users" \
    -H "Content-Type: application/json" \
    -H "Cookie: session=..." \
    -d '{
      "records": [
        { "username": "owner", "email": "owner@example.com", "password": "Str0ng!Pass1", "roles": ["orgowner","admin"] },
        { "username": "viewer", "email": "viewer@example.com", "password": "Str0ng!Pass2", "roles": ["viewer"] }
      ]
    }'
  ```

- Export (CSV):
  ```bash
  curl -G "https://api.example.com/bulk-export/contacts" \
    -H "Cookie: session=..." \
    --data-urlencode "format=csv" \
    --data-urlencode "includeArchived=true" -o contacts.csv
  ```

## Limits

- Max rows per import: **1000**
- Max file size: **10 MB** (configurable)
- Rate limit: **10 requests per 15 minutes** on import/export endpoints. On limit exceeded, returns **429**:
  ```json
  { "message": "Rate limit exceeded for bulk operations" }
  ```

## Responses

- Import (dry-run or live):
  ```json
  {
    "entityType": "contacts",
    "dryRun": true,
    "processed": 2,
    "created": 0,
    "updated": 0,
    "errors": [
      { "row": 1, "field": "email", "message": "email is required for upsert" },
      { "row": 2, "message": "Unexpected columns: foo, bar" }
    ]
  }
  ```
- Export:
  - JSON: `{ "entityType": "contacts", "format": "json", "count": 2, "data": [ ... ] }`
  - CSV: streamed response with headers; `filename` follows `<entity>-export-<timestamp>.csv`.

## Compliance Rules

- `archivedAt`, `piiStripped`, `legalHold` can only be set by privileged roles (Admin/OrgOwner/OrgAdmin/Compliance/ComplianceOfficer/SuperAdmin/PlatformAdmin).
- Entities on `legalHold` cannot be modified.
- PII is redacted on export when `piiStripped` is true.

## Validation Notes

- Required fields enforced per entity (e.g., `email` for users/contacts, `name` for projects/offices).
- Strong password policy for users.
- Roles must be valid enum values.
- Unknown columns rejected per row with an error.

## Extending Bulk to New Entities (Checklist)
- Add entity config in `BulkService.configs`:
  - `modelName`, `schema`, `orgField`, `uniqueField`
  - `allowedFields`, `requiredFields`, `arrayFields`, `piiFields`, `complianceFields`, `exportFields`
- Enforce compliance fields (`archivedAt`, `piiStripped`, `legalHold`) and reject updates when `legalHold` is true.
- Implement upsert helper for the new entity that:
  - Validates required fields and uniqueness.
  - Applies org scoping.
  - Honors `dryRun` semantics.
  - Emits audit via `audit.logMutation`.
- Update docs/tests:
  - Add node:test coverage for import/export, validation, RBAC denial, legal hold, and redaction on export.
  - Update this doc with allowed columns and sample payloads.
