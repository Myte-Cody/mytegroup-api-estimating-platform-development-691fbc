import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { CompanySchema } from '../companies/schemas/company.schema';
import { GraphEdgeSchema } from '../graph-edges/schemas/graph-edge.schema';
import { OfficeSchema } from '../offices/schemas/office.schema';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { PersonSchema } from '../persons/schemas/person.schema';
import { CrmContextController } from './crm-context.controller';
import { CrmContextService } from './crm-context.service';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    MongooseModule.forFeature([
      { name: 'Organization', schema: OrganizationSchema },
      { name: 'Company', schema: CompanySchema },
      { name: 'CompanyLocation', schema: CompanyLocationSchema },
      { name: 'Person', schema: PersonSchema },
      { name: 'Office', schema: OfficeSchema },
      { name: 'GraphEdge', schema: GraphEdgeSchema },
    ]),
  ],
  controllers: [CrmContextController],
  providers: [CrmContextService],
  exports: [CrmContextService],
})
export class CrmContextModule {}
