import { IsBoolean, IsIn, IsOptional, IsString } from 'class-validator';

export class UpdateOrganizationDatastoreDto {
  @IsOptional()
  @IsBoolean()
  useDedicatedDb?: boolean;

  @IsOptional()
  @IsString()
  databaseUri?: string;

  @IsOptional()
  @IsString()
  databaseName?: string;

  @IsOptional()
  @IsIn(['shared', 'dedicated'])
  dataResidency?: 'shared' | 'dedicated';
}
