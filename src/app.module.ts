import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from './common/common.module';
import { AuthModule } from './features/auth/auth.module';
import { EmailModule } from './features/email/email.module';
import { OrganizationsModule } from './features/organizations/organizations.module';
import { UsersModule } from './features/users/users.module';
import { OfficesModule } from './features/offices/offices.module';
import { ProjectsModule } from './features/projects/projects.module';
import { EstimatesModule } from './features/estimates/estimates.module';
import { EventLogModule } from './common/events/event-log.module';
import { mongoConfig } from './config/app.config';
import { ContactsModule } from './features/contacts/contacts.module';
import { TenancyModule } from './common/tenancy/tenancy.module';
import { InvitesModule } from './features/invites/invites.module';
import { MigrationsModule } from './features/migrations/migrations.module';
import { BulkModule } from './features/bulk/bulk.module';
import { ComplianceModule } from './features/compliance/compliance.module';
import { RbacModule } from './features/rbac/rbac.module';
import { LegalModule } from './features/legal/legal.module';
import { WaitlistModule } from './features/waitlist/waitlist.module';
import { APP_GUARD } from '@nestjs/core';
import { LegalGuard } from './features/legal/legal.guard';
import { SessionsModule } from './features/sessions/sessions.module';
import { ContactInquiriesModule } from './features/contact-inquiries/contact-inquiries.module';
import { PeopleModule } from './features/people/people.module';
import { SeatsModule } from './features/seats/seats.module';
import { CompaniesModule } from './features/companies/companies.module';
import { CompanyLocationsModule } from './features/company-locations/company-locations.module';
import { PersonsModule } from './features/persons/persons.module';
import { OrgTaxonomyModule } from './features/org-taxonomy/org-taxonomy.module';
import { GraphEdgesModule } from './features/graph-edges/graph-edges.module';
import { CrmContextModule } from './features/crm-context/crm-context.module';
import { IngestionModule } from './features/ingestion/ingestion.module';
import { DevSeedModule } from './features/dev-seed/dev-seed.module';

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
    EstimatesModule,
    ContactsModule,
    InvitesModule,
    MigrationsModule,
    BulkModule,
    ComplianceModule,
    RbacModule,
    LegalModule,
    WaitlistModule,
    SessionsModule,
    ContactInquiriesModule,
    PeopleModule,
    SeatsModule,
    CompaniesModule,
    CompanyLocationsModule,
    PersonsModule,
    OrgTaxonomyModule,
    GraphEdgesModule,
    CrmContextModule,
    IngestionModule,
    DevSeedModule,
  ],
  providers: [
    {
      provide: APP_GUARD,
      useClass: LegalGuard,
    },
  ],
})
export class AppModule {}
