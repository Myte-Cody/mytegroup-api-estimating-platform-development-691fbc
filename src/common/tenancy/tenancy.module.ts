import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { OrganizationSchema } from '../../features/organizations/schemas/organization.schema';
import { TenantConnectionService } from './tenant-connection.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: 'Organization', schema: OrganizationSchema }])],
  providers: [TenantConnectionService],
  exports: [TenantConnectionService],
})
export class TenancyModule {}
