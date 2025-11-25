import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { ContactsController } from './contacts.controller';
import { ContactsService } from './contacts.service';
import { ContactSchema } from './schemas/contact.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    MongooseModule.forFeature([{ name: 'Contact', schema: ContactSchema }]),
  ],
  controllers: [ContactsController],
  providers: [ContactsService],
  exports: [ContactsService],
})
export class ContactsModule {}
