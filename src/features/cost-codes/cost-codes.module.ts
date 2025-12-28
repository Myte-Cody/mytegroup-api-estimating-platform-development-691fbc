import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { CostCodesController } from './cost-codes.controller';
import { CostCodesService } from './cost-codes.service';
import { CostCodeSchema } from './schemas/cost-code.schema';
import { CostCodeImportJobSchema } from './schemas/cost-code-import-job.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    MongooseModule.forFeature([
      { name: 'CostCode', schema: CostCodeSchema },
      { name: 'CostCodeImportJob', schema: CostCodeImportJobSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [CostCodesController],
  providers: [CostCodesService],
  exports: [CostCodesService],
})
export class CostCodesModule {}
