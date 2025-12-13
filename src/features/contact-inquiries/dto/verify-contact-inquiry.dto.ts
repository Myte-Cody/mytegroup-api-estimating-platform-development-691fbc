import { Transform } from 'class-transformer'
import { IsEmail, IsOptional, IsString, MaxLength, MinLength } from 'class-validator'

export class VerifyContactInquiryDto {
  @IsOptional()
  @IsString()
  @MinLength(2)
  @MaxLength(120)
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  name?: string

  @IsEmail()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  email: string

  @IsOptional()
  @IsString()
  trap?: string
}
