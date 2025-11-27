import { IsDate, IsEnum, IsOptional, IsString, MinLength } from 'class-validator';
import { Type } from 'class-transformer';
import { LegalDocType } from '../legal.types';

export class CreateLegalDocDto {
  @IsEnum(LegalDocType)
  type: LegalDocType;

  @IsString()
  @MinLength(1)
  version: string;

  @IsString()
  @MinLength(10)
  content: string;

  @IsOptional()
  @Type(() => Date)
  @IsDate()
  effectiveAt?: Date;
}
