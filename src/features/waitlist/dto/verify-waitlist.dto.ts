import { IsEmail, IsString, Length } from 'class-validator'

export class VerifyWaitlistDto {
  @IsEmail()
  email: string

  @IsString()
  @Length(4, 12)
  code: string
}
