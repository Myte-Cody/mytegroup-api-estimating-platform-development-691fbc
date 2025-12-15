export const COMPLIANCE_ENTITY_TYPES = ['user', 'contact', 'invite', 'project', 'estimate'] as const;

export type ComplianceEntityType = (typeof COMPLIANCE_ENTITY_TYPES)[number];
