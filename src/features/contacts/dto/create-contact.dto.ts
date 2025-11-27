import { Transform } from 'class-transformer';
import { IsArray, IsEmail, IsEnum, IsOptional, IsString, Matches } from 'class-validator';
import { Role } from '../../../common/roles';

const PHONE_REGEX = /^[0-9+()\-.\s]{7,20}$/;

export class CreateContactDto {
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  name: string;

  @IsOptional()
  @IsEmail()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  email?: string;

  @IsOptional()
  @IsString()
  @Matches(PHONE_REGEX, { message: 'phone must be a valid phone number' })
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  phone?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
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
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  notes?: string;
}
