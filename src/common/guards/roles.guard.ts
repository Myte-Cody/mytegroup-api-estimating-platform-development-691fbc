import { Injectable, CanActivate, ExecutionContext } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Role } from '../roles';
@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private reflector: Reflector) {}
  canActivate(context: ExecutionContext): boolean {
    const roles = this.reflector.get<string[]>('roles', context.getHandler());
    if (!roles) return true;
    const request = context.switchToHttp().getRequest();
    const user = request.user || request.session?.user;
    if (!user || !user.role) return false;
    if (user.role === Role.SuperAdmin) {
      return true;
    }
    if (!user.orgId && roles.length > 0) return false;
    if (roles.includes(user.role)) {
      request.user = user;
      return true;
    }
    const elevatedAdminMatch = roles.includes(Role.Admin) && user.role === Role.OrgOwner;
    if (elevatedAdminMatch) {
      request.user = user;
      return true;
    }
    return false;
  }
}
