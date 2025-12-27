import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { CompanySchema } from '../companies/schemas/company.schema';
import { OrgTaxonomyModule } from '../org-taxonomy/org-taxonomy.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { CompanyLocationsController } from './company-locations.controller';
import { CompanyLocationsService } from './company-locations.service';
import { CompanyLocationSchema } from './schemas/company-location.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    OrgTaxonomyModule,
    MongooseModule.forFeature([
      { name: 'CompanyLocation', schema: CompanyLocationSchema },
      { name: 'Company', schema: CompanySchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [CompanyLocationsController],
  providers: [CompanyLocationsService],
  exports: [CompanyLocationsService],
})
export class CompanyLocationsModule {}
