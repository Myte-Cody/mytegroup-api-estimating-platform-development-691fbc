import { IsBoolean, IsEmail, IsOptional, IsString, MaxLength } from 'class-validator'

export class StartWaitlistDto {
  @IsEmail()
  email: string

  @IsString()
  @MaxLength(120)
  name: string

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
  captchaToken?: string

  @IsOptional()
  @IsString()
  trap?: string
}
