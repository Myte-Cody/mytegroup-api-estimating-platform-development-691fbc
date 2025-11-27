import { IsIn, IsString } from 'class-validator';
import { COMPLIANCE_ENTITY_TYPES, ComplianceEntityType } from '../compliance.types';

export class StripPiiDto {
  @IsString()
  @IsIn([...COMPLIANCE_ENTITY_TYPES])
  entityType: ComplianceEntityType;

  @IsString()
  entityId: string;
}
