import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { OrgTaxonomyModule } from '../org-taxonomy/org-taxonomy.module';
import { OfficesController } from './offices.controller';
import { OfficesService } from './offices.service';
import { OfficeSchema } from './schemas/office.schema';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    OrgTaxonomyModule,
    MongooseModule.forFeature([
      { name: 'Office', schema: OfficeSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [OfficesController],
  providers: [OfficesService],
  exports: [OfficesService],
})
export class OfficesModule {}
