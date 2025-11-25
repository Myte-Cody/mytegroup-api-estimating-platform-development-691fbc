import { IsBoolean, IsIn, IsOptional, IsString } from 'class-validator';

export class CreateOrganizationDto {
  @IsString()
  name: string;

  @IsOptional()
  @IsString()
  databaseUri?: string;

  @IsOptional()
  @IsString()
  databaseName?: string;

  @IsOptional()
  @IsBoolean()
  useDedicatedDb?: boolean;

  @IsOptional()
  @IsIn(['shared', 'dedicated'])
  dataResidency?: 'shared' | 'dedicated';
}
