import { Transform } from 'class-transformer';
import { IsArray, IsObject, IsOptional, IsString } from 'class-validator';

export class UpdateProjectDto {
  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  name?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  description?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  officeId?: string | null;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  projectCode?: string;

  @IsOptional()
  @IsString()
  status?: string;

  @IsOptional()
  @IsString()
  location?: string;

  @IsOptional()
  @IsString()
  bidDate?: string;

  @IsOptional()
  @IsString()
  awardDate?: string;

  @IsOptional()
  @IsString()
  fabricationStartDate?: string;

  @IsOptional()
  @IsString()
  fabricationEndDate?: string;

  @IsOptional()
  @IsString()
  erectionStartDate?: string;

  @IsOptional()
  @IsString()
  erectionEndDate?: string;

  @IsOptional()
  @IsString()
  completionDate?: string;

  @IsOptional()
  @IsObject()
  budget?: Record<string, any>;

  @IsOptional()
  @IsObject()
  quantities?: Record<string, any>;

  @IsOptional()
  @IsObject()
  staffing?: Record<string, any>;

  @IsOptional()
  @IsArray()
  costCodeBudgets?: Record<string, any>[];
}
