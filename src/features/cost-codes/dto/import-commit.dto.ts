import { Type } from 'class-transformer';
import { IsArray, IsOptional, ValidateNested } from 'class-validator';
import { CostCodeInputDto } from './cost-code-input.dto';

export class CostCodeImportCommitDto {
  @IsOptional()
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => CostCodeInputDto)
  codes?: CostCodeInputDto[];
}
