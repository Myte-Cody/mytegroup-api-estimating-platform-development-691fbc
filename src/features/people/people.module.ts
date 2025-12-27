import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { PersonsModule } from '../persons/persons.module';
import { UsersModule } from '../users/users.module';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { PeopleController } from './people.controller';
import { PeopleImportService } from './people-import.service';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    UsersModule,
    PersonsModule,
    MongooseModule.forFeature([{ name: 'Organization', schema: OrganizationSchema }]),
  ],
  controllers: [PeopleController],
  providers: [PeopleImportService],
})
export class PeopleModule {}
