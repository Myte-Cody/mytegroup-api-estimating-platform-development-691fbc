import { Transform, Type } from 'class-transformer';
import { IsBoolean, IsIn, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';
import { normalizeKey } from '../../../common/utils/normalize.util';

export class ListGraphEdgesQueryDto {
  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  orgId?: string;

  @IsOptional()
  @Transform(({ value }) => value === true || value === 'true' || value === '1')
  @IsBoolean()
  includeArchived?: boolean;

  @IsOptional()
  @IsIn(['person', 'org_location', 'company', 'company_location'])
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  fromNodeType?: 'person' | 'org_location' | 'company' | 'company_location';

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  fromNodeId?: string;

  @IsOptional()
  @IsIn(['person', 'org_location', 'company', 'company_location'])
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  toNodeType?: 'person' | 'org_location' | 'company' | 'company_location';

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  toNodeId?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? normalizeKey(value) : value))
  edgeTypeKey?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Type(() => Number)
  page?: number;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(100)
  @Type(() => Number)
  limit?: number;
}
