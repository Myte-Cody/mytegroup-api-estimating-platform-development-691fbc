import { Transform, Type } from 'class-transformer';
import { IsArray, IsEmail, IsNumber, IsOptional, IsString } from 'class-validator';

export class CreateCompanyDto {
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  name: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  externalId?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  website?: string;

  @IsOptional()
  @IsEmail()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  mainEmail?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  mainPhone?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  companyTypeKeys?: string[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  tagKeys?: string[];

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  rating?: number;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  notes?: string;
}

