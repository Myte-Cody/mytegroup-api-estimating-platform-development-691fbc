import { IsArray, IsEmail, IsObject, IsOptional, IsString } from 'class-validator';

export class SendEmailDto {
  @IsEmail()
  email: string;

  @IsString()
  subject: string;

  @IsOptional()
  @IsString()
  text?: string;

  @IsOptional()
  @IsString()
  body?: string;

  @IsOptional()
  @IsString()
  html?: string;

  @IsOptional()
  @IsString()
  templateName?: string;

  @IsOptional()
  @IsString()
  orgId?: string;

  @IsOptional()
  @IsObject()
  variables?: Record<string, unknown>;

  @IsOptional()
  @IsString()
  mode?: 'test' | 'live';

  @IsOptional()
  @IsArray()
  @IsEmail({}, { each: true })
  bcc?: string[];
}
