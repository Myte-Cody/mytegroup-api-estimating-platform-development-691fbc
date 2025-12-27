import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { OrgTaxonomyController } from './org-taxonomy.controller';
import { OrgTaxonomyService } from './org-taxonomy.service';
import { OrgTaxonomySchema } from './schemas/org-taxonomy.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    MongooseModule.forFeature([
      { name: 'OrgTaxonomy', schema: OrgTaxonomySchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [OrgTaxonomyController],
  providers: [OrgTaxonomyService],
  exports: [OrgTaxonomyService],
})
export class OrgTaxonomyModule {}

