import { Module } from '@nestjs/common'
import { MongooseModule } from '@nestjs/mongoose'
import { CommonModule } from '../../common/common.module'
import { EmailModule } from '../email/email.module'
import { WaitlistModule } from '../waitlist/waitlist.module'
import { ContactInquiriesService } from './contact-inquiries.service'
import { ContactInquirySchema } from './schemas/contact-inquiry.schema'
import { ContactInquiriesController } from './contact-inquiries.controller'

@Module({
  imports: [
    CommonModule,
    EmailModule,
    WaitlistModule,
    MongooseModule.forFeature([{ name: 'ContactInquiry', schema: ContactInquirySchema }]),
  ],
  controllers: [ContactInquiriesController],
  providers: [ContactInquiriesService],
  exports: [ContactInquiriesService],
})
export class ContactInquiriesModule {}
