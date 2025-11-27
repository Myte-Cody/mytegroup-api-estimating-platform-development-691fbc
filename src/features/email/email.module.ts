import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { EmailTemplatesController } from '../email-templates/email-templates.controller';
import { EmailTemplatesService } from '../email-templates/email-templates.service';
import { EmailController } from './email.controller';
import { EmailService } from './email.service';
import { EmailTemplateSchema } from '../email-templates/schemas/email-template.schema';
@Module({
  imports: [CommonModule, TenancyModule, MongooseModule.forFeature([{ name: 'EmailTemplate', schema: EmailTemplateSchema }])],
  controllers: [EmailController, EmailTemplatesController],
  providers: [EmailService, EmailTemplatesService],
  exports: [EmailService, EmailTemplatesService],
})
export class EmailModule {}
