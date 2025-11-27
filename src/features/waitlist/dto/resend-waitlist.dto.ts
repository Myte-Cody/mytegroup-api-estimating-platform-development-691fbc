import { IsEmail, IsOptional, IsString } from 'class-validator'

export class ResendWaitlistDto {
  @IsEmail()
  email: string

  @IsOptional()
  @IsString()
  captchaToken?: string
}
