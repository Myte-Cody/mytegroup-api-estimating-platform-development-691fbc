import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from './common/common.module';
import { AuthModule } from './features/auth/auth.module';
import { EmailModule } from './features/email/email.module';
import { OrganizationsModule } from './features/organizations/organizations.module';
import { UsersModule } from './features/users/users.module';
import { OfficesModule } from './features/offices/offices.module';
import { ProjectsModule } from './features/projects/projects.module';
import { EventLogModule } from './common/events/event-log.module';
import { mongoConfig } from './config/app.config';
import { ContactsModule } from './features/contacts/contacts.module';
import { TenancyModule } from './common/tenancy/tenancy.module';
import { InvitesModule } from './features/invites/invites.module';

@Module({
  imports: [
    MongooseModule.forRoot(mongoConfig.uri, { dbName: mongoConfig.dbName }),
    CommonModule,
    EventLogModule,
    TenancyModule,
    AuthModule,
    EmailModule,
    OrganizationsModule,
    UsersModule,
    OfficesModule,
    ProjectsModule,
    ContactsModule,
    InvitesModule,
  ],
})
export class AppModule {}
