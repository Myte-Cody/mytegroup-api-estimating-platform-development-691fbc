package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.PersonResponseDto;
import com.mytegroup.api.entity.people.Person;
import org.springframework.stereotype.Component;

@Component
public class PersonResponseMapper {
    public PersonResponseDto toDto(Person entity) {
        if (entity == null) {
            return null;
        }
        
        return PersonResponseDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .fullName(entity.getFullName())
                .primaryPhoneE164(entity.getPrimaryPhoneE164())
                .title(entity.getTitle())
                .notes(entity.getNotes())
                .piiStripped(entity.getPiiStripped())
                .legalHold(entity.getLegalHold())
                .archivedAt(entity.getArchivedAt())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

