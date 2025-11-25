import { IsArray, IsEmail, IsEnum, IsOptional, IsString } from 'class-validator';
import { Role } from '../../../common/roles';

export class UpdateContactDto {
  @IsOptional()
  @IsString()
  name?: string;

  @IsOptional()
  @IsEmail()
  email?: string;

  @IsOptional()
  @IsString()
  phone?: string;

  @IsOptional()
  @IsString()
  company?: string;

  @IsOptional()
  @IsArray()
  @IsEnum(Role, { each: true })
  roles?: Role[];

  @IsOptional()
  @IsArray()
  tags?: string[];

  @IsOptional()
  @IsString()
  notes?: string;

  @IsOptional()
  @IsString()
  invitedUserId?: string;

  @IsOptional()
  @IsString()
  inviteStatus?: 'pending' | 'accepted';

  @IsOptional()
  invitedAt?: Date;
}
