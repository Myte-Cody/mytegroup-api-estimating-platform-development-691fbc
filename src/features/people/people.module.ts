import { Module } from '@nestjs/common';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { PersonsModule } from '../persons/persons.module';
import { UsersModule } from '../users/users.module';
import { PeopleController } from './people.controller';
import { PeopleImportService } from './people-import.service';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    UsersModule,
    PersonsModule,
  ],
  controllers: [PeopleController],
  providers: [PeopleImportService],
})
export class PeopleModule {}
