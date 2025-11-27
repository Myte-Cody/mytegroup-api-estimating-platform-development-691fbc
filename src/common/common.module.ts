import { Module } from '@nestjs/common';
import { HealthController } from './health.controller';
import { EventLogModule } from './events/event-log.module';
import { AuditLogService } from './services/audit-log.service';
import { SessionGuard } from './guards/session.guard';
import { OrgScopeGuard } from './guards/org-scope.guard';
import { RolesGuard } from './guards/roles.guard';

@Module({
  imports: [EventLogModule],
  controllers: [HealthController],
  providers: [AuditLogService, SessionGuard, OrgScopeGuard, RolesGuard],
  exports: [AuditLogService, EventLogModule, SessionGuard, OrgScopeGuard, RolesGuard],
})
export class CommonModule {}
