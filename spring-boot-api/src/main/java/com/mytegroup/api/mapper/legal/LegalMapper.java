package com.mytegroup.api.mapper.legal;

import com.mytegroup.api.dto.legal.CreateLegalDocDto;
import com.mytegroup.api.entity.legal.LegalDoc;
import com.mytegroup.api.entity.enums.legal.LegalDocType;
import org.springframework.stereotype.Component;

@Component
public class LegalMapper {
    public LegalDoc toEntity(CreateLegalDocDto dto) {
        LegalDoc doc = new LegalDoc();
        
        // Convert String to LegalDocType enum
        if (dto.getType() != null) {
            doc.setType(LegalDocType.valueOf(dto.getType().toUpperCase()));
        }
        
        doc.setVersion(dto.getVersion());
        doc.setContent(dto.getContent());
        doc.setEffectiveAt(dto.getEffectiveAt() != null ? dto.getEffectiveAt() : java.time.LocalDateTime.now());
        return doc;
    }
}

