import { Type } from 'class-transformer'
import { IsIn, IsNumber, IsOptional } from 'class-validator'
import type { ContactInquiryStatus } from '../schemas/contact-inquiry.schema'

export class ListContactInquiriesDto {
  @IsOptional()
  @IsIn(['new', 'in-progress', 'closed'])
  status?: ContactInquiryStatus

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  page?: number

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  limit?: number
}
