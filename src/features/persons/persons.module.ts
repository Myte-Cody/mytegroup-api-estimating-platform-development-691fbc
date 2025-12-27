import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { CompanySchema } from '../companies/schemas/company.schema';
import { CompanyLocationSchema } from '../company-locations/schemas/company-location.schema';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { OrgTaxonomyModule } from '../org-taxonomy/org-taxonomy.module';
import { OfficeSchema } from '../offices/schemas/office.schema';
import { PersonsController } from './persons.controller';
import { PersonsService } from './persons.service';
import { PersonSchema } from './schemas/person.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    OrgTaxonomyModule,
    MongooseModule.forFeature([
      { name: 'Person', schema: PersonSchema },
      { name: 'Company', schema: CompanySchema },
      { name: 'CompanyLocation', schema: CompanyLocationSchema },
      { name: 'Office', schema: OfficeSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [PersonsController],
  providers: [PersonsService],
  exports: [PersonsService],
})
export class PersonsModule {}
