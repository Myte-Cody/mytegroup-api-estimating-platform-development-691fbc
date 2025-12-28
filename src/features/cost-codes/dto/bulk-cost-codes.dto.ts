import { Type } from 'class-transformer';
import { ArrayNotEmpty, IsArray, ValidateNested } from 'class-validator';
import { CostCodeInputDto } from './cost-code-input.dto';

export class BulkCostCodesDto {
  @IsArray()
  @ArrayNotEmpty()
  @ValidateNested({ each: true })
  @Type(() => CostCodeInputDto)
  codes: CostCodeInputDto[];
}
