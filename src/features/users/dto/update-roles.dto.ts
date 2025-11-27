import { ArrayNotEmpty, IsArray, IsEnum } from 'class-validator';
import { Role } from '../../../common/roles';

export class UpdateUserRolesDto {
  @IsArray()
  @ArrayNotEmpty()
  @IsEnum(Role, { each: true })
  roles: Role[];
}
