package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.ContactInquiryResponseDto;
import com.mytegroup.api.entity.contactinquiries.ContactInquiry;
import org.springframework.stereotype.Component;

@Component
public class ContactInquiryResponseMapper {
    public ContactInquiryResponseDto toDto(ContactInquiry entity) {
        if (entity == null) {
            return null;
        }
        
        return ContactInquiryResponseDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .status(entity.getStatus() != null ? entity.getStatus().getValue() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

