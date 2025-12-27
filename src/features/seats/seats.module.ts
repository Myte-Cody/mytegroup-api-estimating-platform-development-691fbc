import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { SeatsController } from './seats.controller';
import { SeatsService } from './seats.service';
import { SeatSchema } from './schemas/seat.schema';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';

@Module({
  imports: [
    CommonModule,
    MongooseModule.forFeature([
      { name: 'Seat', schema: SeatSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [SeatsController],
  providers: [SeatsService],
  exports: [SeatsService],
})
export class SeatsModule {}
