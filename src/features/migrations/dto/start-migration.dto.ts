import { Transform } from 'class-transformer';
import { IsBoolean, IsIn, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';
import { MigrationDirection } from '../schemas/tenant-migration.schema';

export class StartMigrationDto {
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  orgId: string;

  @IsIn(['shared_to_dedicated', 'dedicated_to_shared'])
  direction: MigrationDirection;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  targetUri?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  targetDbName?: string;

  @IsOptional()
  @IsBoolean()
  dryRun?: boolean;

  @IsOptional()
  @IsBoolean()
  resume?: boolean;

  @IsOptional()
  @IsBoolean()
  overrideLegalHold?: boolean;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(5000)
  chunkSize?: number;
}
