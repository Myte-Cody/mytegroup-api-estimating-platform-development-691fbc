export const COMPLIANCE_ENTITY_TYPES = [
  'user',
  'contact',
  'invite',
  'project',
  'estimate',
  'person',
  'company',
  'company_location',
  'org_location',
  'graph_edge',
  'org_taxonomy',
] as const;

export type ComplianceEntityType = (typeof COMPLIANCE_ENTITY_TYPES)[number];
