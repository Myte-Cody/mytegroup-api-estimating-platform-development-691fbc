package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.EmailTemplateResponseDto;
import com.mytegroup.api.entity.emailtemplates.EmailTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateResponseMapper {
    public EmailTemplateResponseDto toDto(EmailTemplate entity) {
        if (entity == null) {
            return null;
        }
        
        return EmailTemplateResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .orgId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

