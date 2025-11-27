import { IsEnum, IsOptional, IsString } from 'class-validator';
import { LegalDocType } from '../legal.types';

export class AcceptLegalDocDto {
  @IsEnum(LegalDocType)
  docType: LegalDocType;

  @IsOptional()
  @IsString()
  version?: string;
}
