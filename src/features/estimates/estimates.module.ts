import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { ProjectSchema } from '../projects/schemas/project.schema';
import { EstimatesController } from './estimates.controller';
import { EstimatesService } from './estimates.service';
import { EstimateSchema } from './schemas/estimate.schema';

@Module({
  imports: [
    CommonModule,
    MongooseModule.forFeature([
      { name: 'Estimate', schema: EstimateSchema },
      { name: 'Project', schema: ProjectSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [EstimatesController],
  providers: [EstimatesService],
  exports: [EstimatesService],
})
export class EstimatesModule {}

