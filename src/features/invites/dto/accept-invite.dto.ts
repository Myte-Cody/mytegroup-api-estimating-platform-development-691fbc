import { IsString, Matches } from 'class-validator';
import { STRONG_PASSWORD_MESSAGE, STRONG_PASSWORD_REGEX } from '../../auth/dto/password-rules';

export class AcceptInviteDto {
  @IsString()
  token: string;

  @IsString()
  username: string;

  @IsString()
  @Matches(STRONG_PASSWORD_REGEX, { message: STRONG_PASSWORD_MESSAGE })
  password: string;
}
