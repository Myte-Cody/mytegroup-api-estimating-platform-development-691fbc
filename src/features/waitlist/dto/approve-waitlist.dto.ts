import { IsEmail, IsOptional, IsString, MaxLength } from 'class-validator'

export class ApproveWaitlistDto {
  @IsEmail()
  email: string

  @IsOptional()
  @IsString()
  @MaxLength(80)
  cohortTag?: string
}
