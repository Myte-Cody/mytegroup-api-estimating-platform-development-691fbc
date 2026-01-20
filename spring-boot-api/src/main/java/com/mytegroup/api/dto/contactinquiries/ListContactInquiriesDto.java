package com.mytegroup.api.dto.contactinquiries;

import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;

public record ListContactInquiriesDto(
    ContactInquiryStatus status,
    Integer page,
    Integer limit
) {
}

