import { Transform, Type } from 'class-transformer';
import { IsBoolean, IsIn, IsInt, IsOptional, IsString, Max, Min } from 'class-validator';

export const CRM_CONTEXT_ENTITY_TYPES = [
  'company',
  'company_location',
  'person',
  'org_location',
  'graph_edge',
] as const;

export type CrmContextEntityType = (typeof CRM_CONTEXT_ENTITY_TYPES)[number];

export class ListCrmContextDocumentsQueryDto {
  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  orgId?: string;

  @IsString()
  @IsIn(CRM_CONTEXT_ENTITY_TYPES)
  entityType!: CrmContextEntityType;

  @IsOptional()
  @Transform(({ value }) => value === true || value === 'true' || value === '1')
  @IsBoolean()
  includeArchived?: boolean;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Type(() => Number)
  page?: number;

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(250)
  @Type(() => Number)
  limit?: number;
}

