import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { OrgTaxonomyModule } from '../org-taxonomy/org-taxonomy.module';
import { CompaniesController } from './companies.controller';
import { CompaniesImportController } from './companies-import.controller';
import { CompaniesImportService } from './companies-import.service';
import { CompaniesService } from './companies.service';
import { CompanySchema } from './schemas/company.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    OrgTaxonomyModule,
    MongooseModule.forFeature([
      { name: 'Company', schema: CompanySchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [CompaniesController, CompaniesImportController],
  providers: [CompaniesService, CompaniesImportService],
  exports: [CompaniesService],
})
export class CompaniesModule {}
