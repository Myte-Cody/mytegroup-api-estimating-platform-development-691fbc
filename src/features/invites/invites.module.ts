import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { ContactsModule } from '../contacts/contacts.module';
import { EmailModule } from '../email/email.module';
import { UsersModule } from '../users/users.module';
import { InvitesController } from './invites.controller';
import { InvitesService } from './invites.service';
import { InviteSchema } from './schemas/invite.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    ContactsModule,
    EmailModule,
    UsersModule,
    MongooseModule.forFeature([{ name: 'Invite', schema: InviteSchema }]),
  ],
  controllers: [InvitesController],
  providers: [InvitesService],
  exports: [InvitesService],
})
export class InvitesModule {}
