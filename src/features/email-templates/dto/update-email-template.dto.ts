import { IsOptional, IsString } from 'class-validator';

export class UpdateEmailTemplateDto {
  @IsOptional()
  @IsString()
  locale?: string;

  @IsString()
  subject: string;

  @IsString()
  html: string;

  @IsString()
  text: string;
}
