# RBAC & Entity Feature Mapping (Backend)

This API enforces RBAC via `SessionGuard` + `OrgScopeGuard` + `RolesGuard` (and the global `LegalGuard`). Controllers must declare `@Roles` on guarded routes. Use this table as a checklist when adding new endpoints.

| Feature / Endpoint | Roles (minimum) | Notes |
| --- | --- | --- |
| Auth `/auth/users` (list) | admin, org_admin, org_owner, superadmin, platform_admin | Org-scoped unless super/platform admin. |
| Users CRUD/roles | admin, org_admin, org_owner, superadmin, platform_admin | Role changes validated against hierarchy; audit on every mutation. |
| Organizations CRUD/flags | superadmin (datastore/legal/PII), admin/org_owner for metadata | Enforce legal hold/PII flags; audit and tenant reset on datastore change. |
| Projects/Offices/Contacts CRUD | admin, org_admin, org_owner, superadmin, platform_admin | Org-scoped; archive instead of delete; audit on mutations. |
| Invites | admin, org_admin, org_owner, superadmin, platform_admin | Validates roles; single-use tokens; audit on create/resend/accept. |
| RBAC `/rbac/*` | superadmin, platform_admin, org_owner, org_admin, admin | Assign/revoke/list roles with hierarchy checks. |
| Compliance `/compliance/*` | superadmin, platform_admin, admin, org_owner, org_admin, compliance, compliance_officer, security, security_officer | Enforces legal hold/PII; audits all actions. |
| Bulk `/bulk-import`, `/bulk-export` | superadmin, platform_admin, admin, org_owner, org_admin, compliance, compliance_officer | Org-scoped; size/rate limits; PII/legal hold redaction on export. |
| Event logs `/events` | admin, compliance, security, superadmin, platform_admin | Org-scoped; org_owner explicitly blocked. |
| Email/Templates | admin, org_owner, superadmin, compliance (test/preview), platform_admin | Allowlist for test sends; audit previews and sends. |
| Migrations | superadmin only | Chunked copy; audit start/progress/finalize/abort. |
| Legal `/legal/*` | create: superadmin/platform_admin; fetch/accept: authenticated | Versioned PP/T&C; audit publish/accept; enforced by global LegalGuard when configured. |

**Checklist for new endpoints**
- Add `@UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)` (plus feature-specific guards).
- Declare `@Roles(...)` with the minimal role set.
- Ensure DTO validation (`class-validator`) and transform are enabled.
- Include compliance fields (`archivedAt`, `piiStripped`, `legalHold`) where PII exists.
- Emit audit/event logs for create/update/archive/legal/PII operations.
- Add node:test coverage for RBAC deny/allow, validation, compliance, audit emission, and tenant scoping.
