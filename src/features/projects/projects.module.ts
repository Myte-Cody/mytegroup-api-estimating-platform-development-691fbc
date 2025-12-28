import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { ProjectsController } from './projects.controller';
import { ProjectsService } from './projects.service';
import { ProjectSchema } from './schemas/project.schema';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { OfficeSchema } from '../offices/schemas/office.schema';
import { CostCodeSchema } from '../cost-codes/schemas/cost-code.schema';
import { PersonsModule } from '../persons/persons.module';
import { SeatsModule } from '../seats/seats.module';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    PersonsModule,
    SeatsModule,
    MongooseModule.forFeature([
      { name: 'Project', schema: ProjectSchema },
      { name: 'Organization', schema: OrganizationSchema },
      { name: 'Office', schema: OfficeSchema },
      { name: 'CostCode', schema: CostCodeSchema },
    ]),
  ],
  controllers: [ProjectsController],
  providers: [ProjectsService],
  exports: [ProjectsService],
})
export class ProjectsModule {}
