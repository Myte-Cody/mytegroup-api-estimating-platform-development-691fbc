import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { ContactSchema } from '../contacts/schemas/contact.schema';
import { ContactsModule } from '../contacts/contacts.module';
import { InvitesModule } from '../invites/invites.module';
import { UsersModule } from '../users/users.module';
import { PeopleController } from './people.controller';
import { PeopleImportService } from './people-import.service';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    UsersModule,
    ContactsModule,
    InvitesModule,
    MongooseModule.forFeature([{ name: 'Contact', schema: ContactSchema }]),
  ],
  controllers: [PeopleController],
  providers: [PeopleImportService],
})
export class PeopleModule {}

