import { CanActivate, ExecutionContext, ForbiddenException, Injectable, Logger } from '@nestjs/common';
import { LegalService } from './legal.service';

@Injectable()
export class LegalGuard implements CanActivate {
  private readonly logger = new Logger(LegalGuard.name);

  constructor(private readonly legal: LegalService) {}

  private shouldEnforce() {
    const raw = (process.env.LEGAL_ENFORCE ?? '1').toLowerCase();
    return raw === '1' || raw === 'true' || raw === 'yes';
  }

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const user = request.session?.user || request.user;
    if (!user?.id) return true;

    try {
      const status = await this.legal.acceptanceStatus({ id: user.id, orgId: user.orgId });
      request.legalStatus = status;
      request.legalRequired = status.required || [];

      const enforce = this.shouldEnforce();
      const path = request.path || '';
      const isLegalRoute = path.startsWith('/legal');
      const isAuthRoute = path.startsWith('/auth');
      const isHealthRoute = path.startsWith('/health');

      // Auth routes must remain accessible so the client can discover legalRequired and route to /legal.
      if (enforce && status.required?.length && !isLegalRoute && !isAuthRoute && !isHealthRoute) {
        throw new ForbiddenException('Legal acceptance required');
      }
    } catch (err) {
      const enforce = this.shouldEnforce();
      if (enforce) {
        throw err;
      }
      // Non-fatal when not enforcing; log and continue so requests are not blocked.
      this.logger.warn(`Legal guard skipped due to error: ${(err as Error).message}`);
    }

    return true;
  }
}
