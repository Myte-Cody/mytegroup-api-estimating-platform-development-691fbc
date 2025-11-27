import { IsBoolean, IsIn, IsOptional, IsString } from 'class-validator';
import { COMPLIANCE_ENTITY_TYPES, ComplianceEntityType } from '../compliance.types';

export class SetLegalHoldDto {
  @IsString()
  @IsIn([...COMPLIANCE_ENTITY_TYPES])
  entityType: ComplianceEntityType;

  @IsString()
  entityId: string;

  @IsBoolean()
  legalHold: boolean;

  @IsOptional()
  @IsString()
  reason?: string;
}
