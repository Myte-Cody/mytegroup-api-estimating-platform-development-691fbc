import { IsBoolean, IsEmail, IsOptional, IsString, Matches, MaxLength } from 'class-validator'

export class StartWaitlistDto {
  @IsEmail()
  email: string

  @IsString()
  @MaxLength(120)
  name: string

  @IsString()
  @Matches(/^\+[1-9]\d{1,14}$/, { message: 'phone must be in E.164 format (e.g. +15145551234)' })
  @MaxLength(20)
  phone: string

  @IsString()
  @MaxLength(80)
  role: string

  @IsOptional()
  @IsString()
  @MaxLength(120)
  source?: string

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
