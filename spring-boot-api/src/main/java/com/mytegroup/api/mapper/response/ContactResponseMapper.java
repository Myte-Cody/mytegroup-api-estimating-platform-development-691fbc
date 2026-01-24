package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.ContactResponseDto;
import com.mytegroup.api.entity.people.Contact;
import org.springframework.stereotype.Component;

@Component
public class ContactResponseMapper {
    public ContactResponseDto toDto(Contact entity) {
        if (entity == null) {
            return null;
        }
        
        return ContactResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .personType(entity.getPersonType() != null ? entity.getPersonType().getValue() : null)
                .notes(entity.getNotes())
                .ironworkerNumber(entity.getIronworkerNumber())
                .company(entity.getCompany())
                .piiStripped(entity.getPiiStripped())
                .legalHold(entity.getLegalHold())
                .archivedAt(entity.getArchivedAt())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

