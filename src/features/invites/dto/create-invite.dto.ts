import { IsEmail, IsEnum, IsOptional, IsString, Max, Min } from 'class-validator';
import { Role } from '../../../common/roles';

export class CreateInviteDto {
  @IsEmail()
  email: string;

  @IsEnum(Role)
  role: Role;

  @IsOptional()
  @IsString()
  contactId?: string;

  @IsOptional()
  @Min(1)
  @Max(168)
  expiresInHours?: number;
}
