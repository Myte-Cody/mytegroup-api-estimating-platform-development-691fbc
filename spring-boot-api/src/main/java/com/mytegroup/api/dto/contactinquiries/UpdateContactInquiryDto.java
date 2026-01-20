package com.mytegroup.api.dto.contactinquiries;

import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;

public record UpdateContactInquiryDto(
    ContactInquiryStatus status,
    String responderId
) {
    public UpdateContactInquiryDto {
        if (responderId != null) {
            responderId = responderId.trim();
        }
    }
}

