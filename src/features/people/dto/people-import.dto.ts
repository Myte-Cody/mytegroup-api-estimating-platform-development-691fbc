import { Transform, Type } from 'class-transformer';
import {
  ArrayMaxSize,
  IsArray,
  IsDateString,
  IsEmail,
  IsEnum,
  IsIn,
  IsInt,
  IsOptional,
  IsString,
  Min,
  ValidateNested,
} from 'class-validator';
import { Role } from '../../../common/roles';
import { ContactCertificationDto } from '../../contacts/dto/create-contact.dto';

export class PeopleImportRowDto {
  @IsInt()
  @Min(1)
  row: number;

  @IsOptional()
  @IsIn(['staff', 'ironworker', 'external', 'internal_staff', 'internal_union', 'external_person'])
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  personType?: 'staff' | 'ironworker' | 'external' | 'internal_staff' | 'internal_union' | 'external_person';

  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  name: string;

  @IsOptional()
  @IsEmail()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  email?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  phone?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  company?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  ironworkerNumber?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  unionLocal?: string;

  @IsOptional()
  @IsDateString()
  dateOfBirth?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  skills?: string[];

  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => ContactCertificationDto)
  certifications?: ContactCertificationDto[];

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  notes?: string;

  @IsOptional()
  @IsEnum(Role)
  inviteRole?: Role;
}

export class PeopleImportPreviewDto {
  @IsArray()
  @ArrayMaxSize(1000)
  @ValidateNested({ each: true })
  @Type(() => PeopleImportRowDto)
  rows: PeopleImportRowDto[];
}

export class PeopleImportConfirmRowDto extends PeopleImportRowDto {
  @IsIn(['create', 'update', 'skip'])
  action: 'create' | 'update' | 'skip';

  @IsOptional()
  @IsString()
  personId?: string;

  @IsOptional()
  @IsString()
  contactId?: string;
}

export class PeopleImportConfirmDto {
  @IsArray()
  @ArrayMaxSize(1000)
  @ValidateNested({ each: true })
  @Type(() => PeopleImportConfirmRowDto)
  rows: PeopleImportConfirmRowDto[];
}
