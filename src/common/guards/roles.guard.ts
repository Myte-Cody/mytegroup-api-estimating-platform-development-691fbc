import { Injectable, CanActivate, ExecutionContext } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Role, expandRoles, mergeRoles, resolvePrimaryRole } from '../roles';

@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const requiredRoles = this.reflector.getAllAndOverride<Role[]>('roles', [
      context.getHandler(),
      context.getClass(),
    ]);
    if (!requiredRoles || requiredRoles.length === 0) return true;

    const request = context.switchToHttp().getRequest();
    const user = request.user || request.session?.user;
    if (!user) return false;

    const userRoles = mergeRoles(user.role as Role, user.roles as Role[]);
    const effectiveRoles = expandRoles(userRoles);
    const privileged = effectiveRoles.some(
      (role) => role === Role.SuperAdmin || role === Role.PlatformAdmin
    );

    if (!user.orgId && requiredRoles.length > 0 && !privileged) return false;
    const hasRequired = requiredRoles.some((role) => effectiveRoles.includes(role));
    if (hasRequired) {
      request.user = {
        ...user,
        roles: userRoles,
        role: user.role || resolvePrimaryRole(userRoles),
      };
      return true;
    }
    return false;
  }
}
