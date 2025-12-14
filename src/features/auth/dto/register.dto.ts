import { IsBoolean, IsEmail, IsEnum, IsOptional, IsString, Matches } from 'class-validator';
import { STRONG_PASSWORD_MESSAGE, STRONG_PASSWORD_REGEX } from './password-rules';
import { Role } from '../../../common/roles';

export class RegisterDto {
  @IsOptional()
  @IsString()
  firstName?: string;

  @IsOptional()
  @IsString()
  lastName?: string;

  @IsString()
  username: string;

  @IsEmail()
  email: string;

  @IsString()
  @Matches(STRONG_PASSWORD_REGEX, { message: STRONG_PASSWORD_MESSAGE })
  password: string;

  @IsOptional()
  @IsString()
  organizationId?: string;

  @IsOptional()
  @IsString()
  organizationName?: string;

  @IsOptional()
  @IsEnum(Role)
  role?: Role;

  @IsOptional()
  @IsString()
  inviteToken?: string;

  @IsBoolean()
  legalAccepted: boolean;

  @IsOptional()
  @IsBoolean()
  orgLegalReconfirm?: boolean;
}
