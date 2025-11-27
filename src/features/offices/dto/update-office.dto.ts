import { Transform } from 'class-transformer';
import { IsOptional, IsString } from 'class-validator';

export class UpdateOfficeDto {
  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  name?: string;

  @IsOptional()
  @IsString()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  address?: string;
}
