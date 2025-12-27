import { Module } from '@nestjs/common';
import { CommonModule } from '../../common/common.module';
import { IngestionContactsController } from './ingestion-contacts.controller';
import { IngestionContactsService } from './ingestion-contacts.service';

@Module({
  imports: [CommonModule],
  controllers: [IngestionContactsController],
  providers: [IngestionContactsService],
})
export class IngestionModule {}

