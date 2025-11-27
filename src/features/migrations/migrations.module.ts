import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { MigrationsController } from './migrations.controller';
import { MigrationsService } from './migrations.service';
import { TenantMigrationSchema } from './schemas/tenant-migration.schema';

@Module({
  imports: [
    CommonModule,
    MongooseModule.forFeature([
      { name: 'TenantMigration', schema: TenantMigrationSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [MigrationsController],
  providers: [MigrationsService],
  exports: [MigrationsService],
})
export class MigrationsModule {}
