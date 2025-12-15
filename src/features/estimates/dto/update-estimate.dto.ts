import { Transform, Type } from 'class-transformer';
import { IsArray, IsIn, IsOptional, IsString, ValidateNested } from 'class-validator';
import { EstimateLineItemDto } from './estimate-line-item.dto';

export class UpdateEstimateDto {
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
  notes?: string;

  @IsOptional()
  @IsIn(['draft', 'final'])
  status?: 'draft' | 'final';

  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => EstimateLineItemDto)
  lineItems?: EstimateLineItemDto[];
}

