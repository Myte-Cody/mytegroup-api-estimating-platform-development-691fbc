import { Transform, Type } from 'class-transformer';
import {
  ArrayMaxSize,
  IsArray,
  IsEmail,
  IsEnum,
  IsIn,
  IsInt,
  IsNumber,
  IsOptional,
  IsString,
  Matches,
} from 'class-validator';
import { Role } from '../../../common/roles';

const PHONE_REGEX = /^[0-9+()\-.\s]{7,25}$/;

export class PeopleImportV1RowDto {
  @IsInt()
  @Type(() => Number)
  row: number;

  @IsIn(['internal_staff', 'internal_union', 'external_person'])
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  personType: 'internal_staff' | 'internal_union' | 'external_person';

  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  displayName: string;

  @IsOptional()
  @IsArray()
  @ArrayMaxSize(10)
  @IsEmail({}, { each: true })
  @Transform(({ value }) =>
    Array.isArray(value) ? value.map((v) => (typeof v === 'string' ? v.trim().toLowerCase() : v)) : value
  )
  emails?: string[];

  @IsOptional()
  @IsEmail()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  primaryEmail?: string;

  @IsOptional()
  @IsArray()
  @ArrayMaxSize(10)
  @IsString({ each: true })
  @Matches(PHONE_REGEX, { each: true, message: 'phones must contain valid phone numbers' })
  @Transform(({ value }) =>
    Array.isArray(value) ? value.map((v) => (typeof v === 'string' ? v.trim() : v)) : value
  )
  phones?: string[];

  @IsOptional()
  @IsString()
  @Matches(PHONE_REGEX, { message: 'primaryPhone must be a valid phone number' })
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  primaryPhone?: string;

  @IsOptional()
  @IsArray()
  @ArrayMaxSize(100)
  @IsString({ each: true })
  tagKeys?: string[];

  @IsOptional()
  @IsArray()
  @ArrayMaxSize(100)
  @IsString({ each: true })
  skillKeys?: string[];

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  departmentKey?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  title?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  orgLocationName?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  reportsToDisplayName?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  companyExternalId?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  companyName?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  companyLocationExternalId?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  companyLocationName?: string;

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
  @ArrayMaxSize(100)
  @IsString({ each: true })
  certifications?: string[];

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  rating?: number;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  notes?: string;

  @IsOptional()
  @IsEnum(Role)
  @Transform(({ value }) => {
    if (typeof value !== 'string') return value;
    const normalized = value.trim().toLowerCase();
    return normalized ? normalized : undefined;
  })
  inviteRole?: Role;
}

export class PeopleImportV1PreviewDto {
  @IsArray()
  @ArrayMaxSize(1000)
  rows: PeopleImportV1RowDto[];
}

export class PeopleImportV1ConfirmRowDto extends PeopleImportV1RowDto {
  @IsIn(['create', 'update', 'skip'])
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  action: 'create' | 'update' | 'skip';

  @IsOptional()
  @IsString()
  personId?: string;
}

export class PeopleImportV1ConfirmDto {
  @IsArray()
  @ArrayMaxSize(1000)
  rows: PeopleImportV1ConfirmRowDto[];
}
