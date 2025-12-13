import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { LegalController } from './legal.controller';
import { LegalService } from './legal.service';
import { LegalGuard } from './legal.guard';
import { LegalDocSchema } from './schemas/legal-doc.schema';
import { LegalAcceptanceSchema } from './schemas/legal-acceptance.schema';
import { CommonModule } from '../../common/common.module';
import { LegalSeedService } from './legal.seed';

@Module({
  imports: [
    CommonModule,
    MongooseModule.forFeature([
      { name: 'LegalDoc', schema: LegalDocSchema },
      { name: 'LegalAcceptance', schema: LegalAcceptanceSchema },
    ]),
  ],
  controllers: [LegalController],
  providers: [LegalService, LegalGuard, LegalSeedService],
  exports: [LegalService, LegalGuard],
})
export class LegalModule {}
