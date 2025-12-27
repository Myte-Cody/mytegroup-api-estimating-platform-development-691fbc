import { Type } from 'class-transformer'
import { IsInt, IsOptional, IsString, Max, Min } from 'class-validator'

export class ListWaitlistDto {
  @IsOptional()
  @IsString()
  status?: string

  @IsOptional()
  @IsString()
  verifyStatus?: string

  @IsOptional()
  @IsString()
  cohortTag?: string

  @IsOptional()
  @IsString()
  emailContains?: string

  @IsOptional()
  @IsInt()
  @Min(1)
  @Type(() => Number)
  page?: number

  @IsOptional()
  @IsInt()
  @Min(1)
  @Max(100)
  @Type(() => Number)
  limit?: number
}
