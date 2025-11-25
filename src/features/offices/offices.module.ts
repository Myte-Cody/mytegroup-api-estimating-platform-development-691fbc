import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { OfficesController } from './offices.controller';
import { OfficesService } from './offices.service';
import { OfficeSchema } from './schemas/office.schema';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';

@Module({
  imports: [
    CommonModule,
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
