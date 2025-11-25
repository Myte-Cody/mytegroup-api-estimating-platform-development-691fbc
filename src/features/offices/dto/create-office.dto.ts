import { IsOptional, IsString } from 'class-validator';

export class CreateOfficeDto {
  @IsString()
  name: string;

  @IsOptional()
  @IsString()
  organizationId?: string;

  @IsOptional()
  @IsString()
  address?: string;
}
