import { IsBoolean, IsEmail, IsEnum, IsOptional, IsString, Matches } from 'class-validator';
import { STRONG_PASSWORD_MESSAGE, STRONG_PASSWORD_REGEX } from '../../auth/dto/password-rules';
import { Role } from '../../../common/roles';

export class CreateUserDto {
  @IsString()
  username: string;

  @IsEmail()
  email: string;

  @IsString()
  @Matches(STRONG_PASSWORD_REGEX, { message: STRONG_PASSWORD_MESSAGE })
  password: string;

  @IsOptional()
  @IsEnum(Role)
  role?: Role;

  @IsOptional()
  @IsString()
  organizationId?: string;

  @IsOptional()
  @IsString()
  verificationTokenHash?: string | null;

  @IsOptional()
  verificationTokenExpires?: Date | null;

  @IsOptional()
  @IsString()
  resetTokenHash?: string | null;

  @IsOptional()
  resetTokenExpires?: Date | null;

  @IsOptional()
  isEmailVerified?: boolean;

  @IsOptional()
  @IsBoolean()
  isOrgOwner?: boolean;
}
