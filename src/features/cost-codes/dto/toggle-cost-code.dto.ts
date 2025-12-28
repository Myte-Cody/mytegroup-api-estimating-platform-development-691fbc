import { Transform } from 'class-transformer';
import { IsBoolean } from 'class-validator';

export class ToggleCostCodeDto {
  @Transform(({ value }) => value === true || value === 'true' || value === '1')
  @IsBoolean()
  active: boolean;
}
