import { BadRequestException, Injectable } from '@nestjs/common';
import { Role, ROLE_HIERARCHY, ROLE_PRIORITY, mergeRoles } from '../../common/roles';
import { ActorContext, UsersService } from '../users/users.service';

@Injectable()
export class RbacService {
  constructor(private readonly users: UsersService) {}

  hierarchy() {
    return {
      roles: Object.values(Role),
      hierarchy: ROLE_HIERARCHY,
      priority: ROLE_PRIORITY,
    };
  }

  async getUserRoles(userId: string, actor: ActorContext) {
    return this.users.getUserRoles(userId, actor);
  }

  async listUserRoles(orgId: string | undefined, actor: ActorContext) {
    return this.users.listOrgRoles(orgId, actor);
  }

  async updateUserRoles(userId: string, roles: Role[], actor: ActorContext) {
    return this.users.updateRoles(userId, roles, actor);
  }

  async revokeRole(userId: string, role: Role, actor: ActorContext) {
    const user = await this.users.getUserRoles(userId, actor);
    const currentRoles = mergeRoles((user as any).role as Role, (user as any).roles as Role[]);
    const remaining = currentRoles.filter((r) => r !== role);
    if (!remaining.length) {
      throw new BadRequestException('User must retain at least one role');
    }
    return this.users.updateRoles(userId, remaining, actor);
  }
}
