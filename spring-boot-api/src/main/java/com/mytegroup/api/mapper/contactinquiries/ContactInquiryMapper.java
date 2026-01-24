package com.mytegroup.api.mapper.contactinquiries;

import com.mytegroup.api.dto.contactinquiries.CreateContactInquiryDto;
import com.mytegroup.api.entity.communication.ContactInquiry;
import org.springframework.stereotype.Component;

@Component
public class ContactInquiryMapper {

    /**
     * Maps CreateContactInquiryDto to ContactInquiry entity.
     */
    public ContactInquiry toEntity(CreateContactInquiryDto dto) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setName(dto.getName());
        inquiry.setEmail(dto.getEmail());
        inquiry.setMessage(dto.getMessage());
        inquiry.setSource(dto.getSource());
        return inquiry;
    }
}

