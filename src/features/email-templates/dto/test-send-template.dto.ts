import { IsEmail, IsOptional, IsString } from 'class-validator';
import { PreviewEmailTemplateDto } from './preview-email-template.dto';

export class TestSendTemplateDto extends PreviewEmailTemplateDto {
  @IsEmail()
  to: string;

  @IsOptional()
  @IsString()
  locale?: string;
}
