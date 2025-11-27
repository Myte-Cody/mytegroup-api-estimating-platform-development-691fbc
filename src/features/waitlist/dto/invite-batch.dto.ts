import { IsNumber, IsOptional, IsString, Max, Min } from 'class-validator'

export class InviteBatchDto {
  @IsOptional()
  @IsNumber()
  @Min(1)
  @Max(500)
  limit?: number

  @IsOptional()
  @IsString()
  cohortTag?: string
}
