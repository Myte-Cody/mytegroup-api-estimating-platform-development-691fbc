import { IsBoolean } from 'class-validator';

export class UpdateOrganizationLegalHoldDto {
  @IsBoolean()
  legalHold: boolean;
}
