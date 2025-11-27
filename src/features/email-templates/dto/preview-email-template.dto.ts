import { IsObject, IsOptional, IsString } from 'class-validator';

export class PreviewEmailTemplateDto {
  @IsOptional()
  @IsString()
  locale?: string;

  @IsObject()
  variables: Record<string, unknown>;
}
