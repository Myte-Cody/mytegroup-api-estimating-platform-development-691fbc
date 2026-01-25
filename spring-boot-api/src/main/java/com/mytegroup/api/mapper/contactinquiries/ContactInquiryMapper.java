package com.mytegroup.api.mapper.contactinquiries;

import com.mytegroup.api.dto.contactinquiries.CreateContactInquiryDto;
import com.mytegroup.api.dto.response.ContactInquiryResponseDto;
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

    /**
     * Maps ContactInquiry entity to ContactInquiryResponseDto.
     */
    public ContactInquiryResponseDto toDto(ContactInquiry entity) {
        if (entity == null) {
            return null;
        }
        
        return ContactInquiryResponseDto.builder()
                .id(entity.getId())
                .firstName(entity.getName())
                .email(entity.getEmail())
                .status(entity.getStatus() != null ? entity.getStatus().getValue() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

