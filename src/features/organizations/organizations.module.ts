import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { OrganizationsController } from './organizations.controller';
import { OrganizationsService } from './organizations.service';
import { OrganizationSchema } from './schemas/organization.schema';
@Module({
  imports: [CommonModule, TenancyModule, MongooseModule.forFeature([{ name: 'Organization', schema: OrganizationSchema }])],
  controllers: [OrganizationsController],
  providers: [OrganizationsService],
  exports: [OrganizationsService],
})
export class OrganizationsModule {}
