import { IsString, Length } from 'class-validator'

export class RevokeSessionDto {
  @IsString()
  @Length(5, 200)
  sessionId: string
}
