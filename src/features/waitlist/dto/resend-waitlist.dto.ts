import { IsEmail } from 'class-validator'

export class ResendWaitlistDto {
  @IsEmail()
  email: string
}
