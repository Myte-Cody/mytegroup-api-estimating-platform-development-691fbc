import { IsObject, IsOptional, IsString, MaxLength } from 'class-validator';

export class WaitlistEventDto {
  @IsString()
  @MaxLength(80)
  event: string;

  @IsOptional()
  @IsObject()
  meta?: Record<string, any>;

  @IsOptional()
  @IsString()
  source?: string;

  @IsOptional()
  @IsString()
  path?: string;
}
