import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { NotificationsController } from './notifications.controller';
import { NotificationsService } from './notifications.service';
import { NotificationSchema } from './schemas/notification.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    MongooseModule.forFeature([
      { name: 'Notification', schema: NotificationSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [NotificationsController],
  providers: [NotificationsService],
  exports: [NotificationsService],
})
export class NotificationsModule {}
