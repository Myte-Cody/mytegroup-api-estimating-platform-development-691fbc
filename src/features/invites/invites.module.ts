import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { EmailModule } from '../email/email.module';
import { UsersModule } from '../users/users.module';
import { SeatsModule } from '../seats/seats.module';
import { PersonsModule } from '../persons/persons.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { InvitesController } from './invites.controller';
import { InvitesService } from './invites.service';
import { InviteSchema } from './schemas/invite.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    PersonsModule,
    EmailModule,
    UsersModule,
    SeatsModule,
    MongooseModule.forFeature([
      { name: 'Invite', schema: InviteSchema },
      { name: 'Organization', schema: OrganizationSchema },
    ]),
  ],
  controllers: [InvitesController],
  providers: [InvitesService],
  exports: [InvitesService],
})
export class InvitesModule {}
