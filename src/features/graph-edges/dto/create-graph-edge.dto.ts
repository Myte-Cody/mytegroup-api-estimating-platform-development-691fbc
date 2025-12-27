import { Transform } from 'class-transformer';
import { IsIn, IsObject, IsOptional, IsString, ValidateIf, IsDateString } from 'class-validator';
import { normalizeKey } from '../../../common/utils/normalize.util';

export class CreateGraphEdgeDto {
  @IsIn(['person', 'org_location', 'company', 'company_location'])
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  fromNodeType: 'person' | 'org_location' | 'company' | 'company_location';

  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  fromNodeId: string;

  @IsIn(['person', 'org_location', 'company', 'company_location'])
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  toNodeType: 'person' | 'org_location' | 'company' | 'company_location';

  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  toNodeId: string;

  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? normalizeKey(value) : value))
  edgeTypeKey: string;

  @IsOptional()
  @ValidateIf((_obj, value) => value !== undefined)
  @IsObject()
  metadata?: Record<string, any>;

  @IsOptional()
  @IsDateString()
  effectiveFrom?: string;

  @IsOptional()
  @IsDateString()
  effectiveTo?: string;
}

