import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { LegalService } from './legal.service';

@Injectable()
export class LegalSeedService implements OnModuleInit {
  private readonly logger = new Logger(LegalSeedService.name);

  constructor(private readonly legal: LegalService) {}

  async onModuleInit() {
    try {
      await this.legal.ensureDefaultDocs();
      this.logger.log('Default legal docs ensured.');
    } catch (err) {
      this.logger.warn(`Failed to seed legal docs: ${(err as Error)?.message || err}`);
    }
  }
}

