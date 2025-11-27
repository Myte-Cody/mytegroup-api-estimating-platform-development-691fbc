import { CanActivate, ExecutionContext, ForbiddenException, Injectable, Logger } from '@nestjs/common';
import { LegalService } from './legal.service';

@Injectable()
export class LegalGuard implements CanActivate {
  private readonly logger = new Logger(LegalGuard.name);

  constructor(private readonly legal: LegalService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const user = request.session?.user || request.user;
    if (!user?.id) return true;

    try {
      const status = await this.legal.acceptanceStatus({ id: user.id, orgId: user.orgId });
      request.legalStatus = status;
      request.legalRequired = status.required || [];

      const enforce = (process.env.LEGAL_ENFORCE || '').toLowerCase() === '1';
      const isLegalRoute = (request.path || '').startsWith('/legal');

      if (enforce && status.required?.length && !isLegalRoute) {
        throw new ForbiddenException('Legal acceptance required');
      }
    } catch (err) {
      const enforce = (process.env.LEGAL_ENFORCE || '').toLowerCase() === '1';
      if (enforce) {
        throw err;
      }
      // Non-fatal when not enforcing; log and continue so requests are not blocked.
      this.logger.warn(`Legal guard skipped due to error: ${(err as Error).message}`);
    }

    return true;
  }
}
