import { IsBoolean } from 'class-validator';

export class UpdateOrganizationPiiDto {
  @IsBoolean()
  piiStripped: boolean;
}
