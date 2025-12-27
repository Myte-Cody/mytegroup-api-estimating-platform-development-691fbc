import { Transform, Type } from 'class-transformer';
import { ArrayMaxSize, IsArray, IsInt, IsObject, IsOptional, IsString, Matches, ValidateNested } from 'class-validator';
import { normalizeKey } from '../../../common/utils/normalize.util';

export class PutOrgTaxonomyValueDto {
  @IsString()
  @Transform(({ value }) => normalizeKey(typeof value === 'string' ? value : ''))
  @Matches(/^[a-z0-9_]+$/, { message: 'key must be lowercase snake_case' })
  key: string;

  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  label: string;

  @IsOptional()
  @IsInt()
  @Type(() => Number)
  sortOrder?: number;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  color?: string;

  @IsOptional()
  @IsObject()
  metadata?: Record<string, any>;
}

export class PutOrgTaxonomyDto {
  @IsArray()
  @ArrayMaxSize(500)
  @ValidateNested({ each: true })
  @Type(() => PutOrgTaxonomyValueDto)
  values: PutOrgTaxonomyValueDto[];
}

