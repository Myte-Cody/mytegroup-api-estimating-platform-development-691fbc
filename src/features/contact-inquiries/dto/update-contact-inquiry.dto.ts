import { IsIn, IsOptional, IsString } from 'class-validator'
import type { ContactInquiryStatus } from '../schemas/contact-inquiry.schema'

export class UpdateContactInquiryDto {
  @IsOptional()
  @IsIn(['new', 'in-progress', 'closed'])
  status?: ContactInquiryStatus

  @IsOptional()
  @IsString()
  responderId?: string
}

