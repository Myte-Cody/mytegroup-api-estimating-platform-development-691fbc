import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { LegalController } from './legal.controller';
import { LegalService } from './legal.service';
import { LegalGuard } from './legal.guard';
import { LegalDocSchema } from './schemas/legal-doc.schema';
import { LegalAcceptanceSchema } from './schemas/legal-acceptance.schema';
import { CommonModule } from '../../common/common.module';

@Module({
  imports: [
    CommonModule,
    MongooseModule.forFeature([
      { name: 'LegalDoc', schema: LegalDocSchema },
      { name: 'LegalAcceptance', schema: LegalAcceptanceSchema },
    ]),
  ],
  controllers: [LegalController],
  providers: [LegalService, LegalGuard],
  exports: [LegalService, LegalGuard],
})
export class LegalModule {}
