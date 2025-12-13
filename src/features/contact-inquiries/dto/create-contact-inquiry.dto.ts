import { Transform } from 'class-transformer'
import { IsBoolean, IsEmail, IsOptional, IsString, MaxLength, MinLength } from 'class-validator'

export class CreateContactInquiryDto {
  @IsString()
  @MinLength(2)
  @MaxLength(120)
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  name: string

  @IsEmail()
  @Transform(({ value }) => (typeof value === 'string' ? value.trim().toLowerCase() : value))
  email: string

  @IsString()
  @MinLength(10)
  @MaxLength(4000)
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  message: string

  @IsOptional()
  @IsString()
  @MaxLength(120)
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  source?: string

  @IsOptional()
  @IsBoolean()
  joinWaitlist?: boolean

  @IsOptional()
  @IsString()
  @MaxLength(80)
  @Transform(({ value }) => (typeof value === 'string' ? value.trim() : value))
  waitlistRole?: string

  @IsOptional()
  @IsBoolean()
  preCreateAccount?: boolean

  @IsOptional()
  @IsBoolean()
  marketingConsent?: boolean

  @IsOptional()
  @IsString()
  trap?: string
}
