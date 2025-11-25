import { IsString } from 'class-validator';

export class PasswordStrengthDto {
  @IsString()
  password: string;
}
