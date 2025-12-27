import { Type } from 'class-transformer';
import { ArrayMaxSize, IsArray, IsBoolean, IsEnum, IsOptional, IsString } from 'class-validator';
import { IngestionContactsProfile } from '../ingestion.types';

export class IngestionContactsSuggestMappingDto {
  @IsOptional()
  @IsEnum(IngestionContactsProfile)
  profile?: IngestionContactsProfile;

  @IsArray()
  @ArrayMaxSize(200)
  @IsString({ each: true })
  @Type(() => String)
  headers: string[];

  @IsOptional()
  @IsBoolean()
  allowAiProcessing?: boolean;
}

