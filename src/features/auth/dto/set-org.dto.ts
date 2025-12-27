import { IsNotEmpty, IsString } from 'class-validator';

export class SetOrgDto {
  @IsString()
  @IsNotEmpty()
  orgId!: string;
}

