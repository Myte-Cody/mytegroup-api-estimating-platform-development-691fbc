export enum Role {
  SuperAdmin = 'superadmin',
  PlatformAdmin = 'platform_admin',
  OrgOwner = 'org_owner',
  OrgAdmin = 'org_admin',
  Manager = 'manager',
  Viewer = 'viewer',
  Admin = 'admin',
  ComplianceOfficer = 'compliance_officer',
  SecurityOfficer = 'security_officer',
  PM = 'pm',
  Estimator = 'estimator',
  Engineer = 'engineer',
  Detailer = 'detailer',
  Transporter = 'transporter',
  Foreman = 'foreman',
  Superintendent = 'superintendent',
  QAQC = 'qaqc',
  HS = 'hs',
  Purchasing = 'purchasing',
  Compliance = 'compliance',
  Security = 'security',
  Finance = 'finance',
  User = 'user',
}

export const ROLE_PRIORITY: Role[] = [
  Role.SuperAdmin,
  Role.PlatformAdmin,
  Role.OrgOwner,
  Role.OrgAdmin,
  Role.Admin,
  Role.Manager,
  Role.ComplianceOfficer,
  Role.SecurityOfficer,
  Role.PM,
  Role.Estimator,
  Role.Engineer,
  Role.Detailer,
  Role.Transporter,
  Role.Foreman,
  Role.Superintendent,
  Role.QAQC,
  Role.HS,
  Role.Purchasing,
  Role.Compliance,
  Role.Security,
  Role.Finance,
  Role.Viewer,
  Role.User,
];

const BASE_ROLES: Role[] = [
  Role.Manager,
  Role.ComplianceOfficer,
  Role.SecurityOfficer,
  Role.PM,
  Role.Estimator,
  Role.Engineer,
  Role.Detailer,
  Role.Transporter,
  Role.Foreman,
  Role.Superintendent,
  Role.QAQC,
  Role.HS,
  Role.Purchasing,
  Role.Compliance,
  Role.Security,
  Role.Finance,
  Role.Viewer,
  Role.User,
];

export const ROLE_HIERARCHY: Record<Role, Role[]> = {
  [Role.SuperAdmin]: [...ROLE_PRIORITY],
  [Role.PlatformAdmin]: ROLE_PRIORITY.filter((role) => role !== Role.SuperAdmin),
  [Role.OrgOwner]: [Role.OrgOwner, Role.OrgAdmin, Role.Admin, ...BASE_ROLES],
  [Role.OrgAdmin]: [Role.OrgAdmin, Role.Admin, ...BASE_ROLES],
  [Role.Admin]: [Role.Admin, ...BASE_ROLES],
  [Role.Manager]: [Role.Manager, Role.Viewer, Role.User],
  [Role.Viewer]: [Role.Viewer, Role.User],
  [Role.ComplianceOfficer]: [Role.ComplianceOfficer, Role.Compliance, Role.User],
  [Role.SecurityOfficer]: [Role.SecurityOfficer, Role.Security, Role.User],
  [Role.PM]: [Role.PM, Role.Viewer, Role.User],
  [Role.Estimator]: [Role.Estimator, Role.Viewer, Role.User],
  [Role.Engineer]: [Role.Engineer, Role.Viewer, Role.User],
  [Role.Detailer]: [Role.Detailer, Role.Viewer, Role.User],
  [Role.Transporter]: [Role.Transporter, Role.Viewer, Role.User],
  [Role.Foreman]: [Role.Foreman, Role.Viewer, Role.User],
  [Role.Superintendent]: [Role.Superintendent, Role.Viewer, Role.User],
  [Role.QAQC]: [Role.QAQC, Role.Viewer, Role.User],
  [Role.HS]: [Role.HS, Role.Viewer, Role.User],
  [Role.Purchasing]: [Role.Purchasing, Role.Viewer, Role.User],
  [Role.Compliance]: [Role.Compliance, Role.Viewer, Role.User],
  [Role.Security]: [Role.Security, Role.Viewer, Role.User],
  [Role.Finance]: [Role.Finance, Role.Viewer, Role.User],
  [Role.User]: [Role.User],
};

export const normalizeRoles = (roles: Role[]) => {
  return Array.from(new Set((roles || []).filter(Boolean)));
};

export const mergeRoles = (primary?: Role, roles?: Role[]) => {
  const merged = [...(roles || []), primary].filter(Boolean) as Role[];
  const normalized = normalizeRoles(merged);
  return normalized.length ? normalized : [Role.User];
};

export const expandRoles = (roles: Role[]) => {
  const normalized = normalizeRoles(roles);
  const expanded = new Set<Role>();
  normalized.forEach((role) => {
    const implied = ROLE_HIERARCHY[role] || [role];
    implied.forEach((r) => expanded.add(r));
  });
  return Array.from(expanded);
};

export const resolvePrimaryRole = (roles: Role[]) => {
  const normalized = mergeRoles(undefined, roles);
  return ROLE_PRIORITY.find((r) => normalized.includes(r)) || Role.User;
};

export const canAssignRoles = (actorRoles: Role[], targetRoles: Role[]) => {
  if (!targetRoles || targetRoles.length === 0) return false;
  const actorEffective = expandRoles(actorRoles);
  const actorTopIdx = ROLE_PRIORITY.findIndex((role) => actorEffective.includes(role));
  if (actorTopIdx === -1) return false;
  const targetTopIdx = ROLE_PRIORITY.findIndex((role) => targetRoles.includes(role));
  if (targetTopIdx === -1) return false;
  return actorTopIdx <= targetTopIdx;
};
