import { ArrayNotEmpty, IsArray, IsBoolean, IsIn, IsOptional, IsString } from 'class-validator';
import { Type } from 'class-transformer';
import { COMPLIANCE_ENTITY_TYPES, ComplianceEntityType } from '../compliance.types';

export class BatchArchiveDto {
  @IsString()
  @IsIn([...COMPLIANCE_ENTITY_TYPES])
  entityType: ComplianceEntityType;

  @IsArray()
  @ArrayNotEmpty()
  @IsString({ each: true })
  entityIds: string[];

  @IsOptional()
  @IsBoolean()
  @Type(() => Boolean)
  archive?: boolean;
}
