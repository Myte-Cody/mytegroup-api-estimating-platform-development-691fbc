import { Type } from 'class-transformer';
import { IsBoolean, IsEnum, IsObject, IsOptional } from 'class-validator';
import { IngestionContactsProfile } from '../ingestion.types';

export class IngestionContactsParseRowDto {
  @IsEnum(IngestionContactsProfile)
  profile: IngestionContactsProfile;

  @IsObject()
  @Type(() => Object)
  cells: Record<string, any>;

  @IsOptional()
  @IsBoolean()
  allowAiProcessing?: boolean;
}

