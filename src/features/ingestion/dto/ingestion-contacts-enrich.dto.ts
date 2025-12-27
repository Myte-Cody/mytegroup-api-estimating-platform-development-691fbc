import { Type } from 'class-transformer';
import {
  ArrayMaxSize,
  IsArray,
  IsBoolean,
  IsEmail,
  IsEnum,
  IsObject,
  IsOptional,
  IsString,
  ValidateNested,
} from 'class-validator';
import { IngestionContactsProfile } from '../ingestion.types';

class IngestionPersonDraftDto {
  @IsOptional()
  @IsString()
  displayName?: string;

  @IsOptional()
  @IsArray()
  @ArrayMaxSize(10)
  @IsEmail({}, { each: true })
  emails?: string[];

  @IsOptional()
  @IsArray()
  @ArrayMaxSize(10)
  @IsString({ each: true })
  phones?: string[];
}

class IngestionContactDraftDto {
  @IsOptional()
  @ValidateNested()
  @Type(() => IngestionPersonDraftDto)
  person?: IngestionPersonDraftDto;

  @IsOptional()
  @IsString()
  companyName?: string;
}

export class IngestionContactsEnrichDto {
  @IsEnum(IngestionContactsProfile)
  profile: IngestionContactsProfile;

  @IsObject()
  @ValidateNested()
  @Type(() => IngestionContactDraftDto)
  candidate: IngestionContactDraftDto;

  @IsOptional()
  @IsBoolean()
  allowAiProcessing?: boolean;
}
