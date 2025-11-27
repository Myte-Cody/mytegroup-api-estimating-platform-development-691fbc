import { Controller, Get, UseGuards } from '@nestjs/common';
import Redis from 'ioredis';
import { Queue } from 'bullmq';
import { redisConfig } from '../config/redis.config';
import { opsConfig } from '../config/ops.config';
import { SessionGuard } from './guards/session.guard';
import { RolesGuard } from './guards/roles.guard';
import { Roles } from './decorators/roles.decorator';
import { Role } from './roles';

@Controller('health')
export class HealthController {
  @Get()
  check() { return { status: 'ok' }; }

  @Get('full')
  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgOwner, Role.SuperAdmin, Role.PlatformAdmin)
  async full() {
    const redisUrl = redisConfig().url;
    const redis = new Redis(redisUrl, { maxRetriesPerRequest: 1, lazyConnect: true, showFriendlyErrorStack: true });
    let redisHealthy = false;
    try {
      await redis.connect();
      await redis.ping();
      redisHealthy = true;
    } catch (err) {
      redisHealthy = false;
    } finally {
      await redis.quit().catch(() => undefined);
    }

    const { bullPrefix } = redisConfig();
    const ops = opsConfig();
    const queue = new Queue('waitlist-mail', { connection: { url: redisUrl }, prefix: bullPrefix });
    const dlq = new Queue('waitlist-mail-dlq', { connection: { url: redisUrl }, prefix: bullPrefix });
    let queueHealth: any = {};
    let dlqSample: any[] = [];
    const flags: Record<string, boolean> = {};
    try {
      queueHealth = await queue.getJobCounts('waiting', 'active', 'failed', 'delayed');
      dlqSample = await dlq.getFailed(0, Math.max(ops.dlqSample, 3));
      flags.waitlistMailBacklog = (queueHealth.waiting || 0) > ops.queueAlertThreshold;
      flags.waitlistMailFailed = (queueHealth.failed || 0) > ops.queueAlertThreshold;
      flags.waitlistMailDlq = (dlqSample?.length || 0) > 0;
    } catch (err) {
      queueHealth = { error: (err as Error)?.message || String(err) };
    } finally {
      await dlq.close().catch(() => undefined);
      await queue.close().catch(() => undefined);
    }

    return {
      status: redisHealthy && !Object.values(flags).some(Boolean) ? 'ok' : 'degraded',
      redis: redisHealthy ? 'ok' : 'unavailable',
      queues: { waitlistMail: queueHealth },
      dlq: dlqSample?.map((j) => ({
        id: j?.id,
        failedReason: j?.failedReason,
        finishedOn: j?.finishedOn,
        timestamp: j?.timestamp,
      })),
      flags,
    };
  }
}
