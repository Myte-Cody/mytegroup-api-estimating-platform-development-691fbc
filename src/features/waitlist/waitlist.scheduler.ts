import { Injectable, Logger, OnModuleDestroy, OnModuleInit } from '@nestjs/common'
import { waitlistConfig } from './waitlist.config'
import { WaitlistService } from './waitlist.service'

@Injectable()
export class WaitlistScheduler implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(WaitlistScheduler.name)
  private timer: NodeJS.Timeout | null = null

  constructor(private readonly waitlist: WaitlistService) {}

  onModuleInit() {
    if (!waitlistConfig.invite.autoInviteEnabled) return
    this.timer = setInterval(() => this.tick(), waitlistConfig.invite.intervalMs)
  }

  onModuleDestroy() {
    if (this.timer) clearInterval(this.timer)
  }

  private async tick() {
    try {
      const result = await this.waitlist.processInviteBatch()
      if (result.invited) {
        this.logger.log(`Auto-invite batch sent ${result.invited} invites`)
      }
    } catch (err) {
      this.logger.error('Auto-invite batch failed', err as any)
    }
  }
}
