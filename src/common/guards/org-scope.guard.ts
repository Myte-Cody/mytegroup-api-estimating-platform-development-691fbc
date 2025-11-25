import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { Role } from '../roles';

@Injectable()
export class OrgScopeGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();
    const user = request.user || request.session?.user;
    if (!user) return false;
    if (user.role === Role.SuperAdmin) return true;
    return !!user.orgId;
  }
}
