import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { Role, expandRoles, mergeRoles } from '../roles';

@Injectable()
export class OrgScopeGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();
    const user = request.user || request.session?.user;
    if (!user) return false;
    const userRoles = mergeRoles(user.role as Role, user.roles as Role[]);
    const effective = expandRoles(userRoles);
    if (effective.includes(Role.SuperAdmin) || effective.includes(Role.PlatformAdmin)) {
      request.user = { ...user, roles: userRoles };
      return true;
    }
    const hasOrg = !!user.orgId;
    if (hasOrg) request.user = { ...user, roles: userRoles };
    return hasOrg;
  }
}
