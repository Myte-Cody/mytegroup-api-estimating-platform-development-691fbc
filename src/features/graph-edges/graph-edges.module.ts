import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { CompanySchema } from '../companies/schemas/company.schema';
import { CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { OfficeSchema } from '../offices/schemas/office.schema';
import { OrgTaxonomyModule } from '../org-taxonomy/org-taxonomy.module';
import { PersonSchema } from '../persons/schemas/person.schema';
import { GraphEdgesController } from './graph-edges.controller';
import { GraphEdgesService } from './graph-edges.service';
import { GraphEdgeSchema } from './schemas/graph-edge.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    OrgTaxonomyModule,
    MongooseModule.forFeature([
      { name: 'GraphEdge', schema: GraphEdgeSchema },
      { name: 'Person', schema: PersonSchema },
      { name: 'Company', schema: CompanySchema },
      { name: 'CompanyLocation', schema: CompanyLocationSchema },
      { name: 'Office', schema: OfficeSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [GraphEdgesController],
  providers: [GraphEdgesService],
  exports: [GraphEdgesService],
})
export class GraphEdgesModule {}

