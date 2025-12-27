import { Transform, Type } from 'class-transformer';
import {
  IsArray,
  IsDateString,
  IsEmail,
  IsIn,
  IsNumber,
  IsOptional,
  IsString,
  Matches,
  ValidateNested,
} from 'class-validator';
import { PersonCertificationDto } from './create-person.dto';

const PHONE_REGEX = /^[0-9+()\-.\s]{7,25}$/;

export class UpdatePersonDto {
  @IsOptional()
  @IsIn(['internal_staff', 'internal_union', 'external_person'])
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  personType?: 'internal_staff' | 'internal_union' | 'external_person';

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  displayName?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  firstName?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  lastName?: string;

  @IsOptional()
  @IsDateString()
  dateOfBirth?: string;

  @IsOptional()
  @IsArray()
  @IsEmail({}, { each: true })
  @Transform(({ value }) => (Array.isArray(value) ? value.map((v) => (typeof v === 'string' ? v.trim() : v)) : value))
  emails?: string[];

  @IsOptional()
  @IsEmail()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  primaryEmail?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @Matches(PHONE_REGEX, { each: true, message: 'phones must contain valid phone numbers' })
  @Transform(({ value }) => (Array.isArray(value) ? value.map((v) => (typeof v === 'string' ? v.trim() : v)) : value))
  phones?: string[];

  @IsOptional()
  @IsString()
  @Matches(PHONE_REGEX, { message: 'primaryPhone must be a valid phone number' })
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  primaryPhone?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  tagKeys?: string[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  skillKeys?: string[];

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  departmentKey?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  orgLocationId?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  reportsToPersonId?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  ironworkerNumber?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  unionLocal?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  skillFreeText?: string[];

  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => PersonCertificationDto)
  certifications?: PersonCertificationDto[];

  @IsOptional()
  @IsNumber()
  rating?: number;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  notes?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  companyId?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  companyLocationId?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  title?: string;
}

