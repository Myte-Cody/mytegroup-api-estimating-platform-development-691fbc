import { IsBoolean, IsIn, IsObject, IsOptional, IsString, MaxLength } from 'class-validator';

export class CreateOrganizationDto {
  @IsString()
  name: string;

  @IsOptional()
  @IsObject()
  metadata?: Record<string, unknown>;

  @IsOptional()
  @IsString()
  databaseUri?: string;

  @IsOptional()
  @IsString()
  datastoreUri?: string;

  @IsOptional()
  @IsString()
  databaseName?: string;

  @IsOptional()
  @IsString()
  @MaxLength(120)
  primaryDomain?: string;

  @IsOptional()
  @IsBoolean()
  useDedicatedDb?: boolean;

  @IsOptional()
  @IsIn(['shared', 'dedicated'])
  datastoreType?: 'shared' | 'dedicated';

  @IsOptional()
  @IsIn(['shared', 'dedicated'])
  dataResidency?: 'shared' | 'dedicated';

  @IsOptional()
  @IsBoolean()
  piiStripped?: boolean;

  @IsOptional()
  @IsBoolean()
  legalHold?: boolean;
}
