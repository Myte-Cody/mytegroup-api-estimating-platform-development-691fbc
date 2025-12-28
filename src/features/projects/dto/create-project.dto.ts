import { IsArray, IsObject, IsOptional, IsString } from 'class-validator';

export class CreateProjectDto {
  @IsString()
  name: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsOptional()
  @IsString()
  orgId?: string;

  @IsOptional()
  @IsString()
  officeId?: string;

  @IsOptional()
  @IsString()
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
