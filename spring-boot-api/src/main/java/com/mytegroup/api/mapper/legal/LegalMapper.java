package com.mytegroup.api.mapper.legal;

import com.mytegroup.api.dto.legal.CreateLegalDocDto;
import com.mytegroup.api.entity.legal.LegalDoc;
import org.springframework.stereotype.Component;

@Component
public class LegalMapper {
    public LegalDoc toEntity(CreateLegalDocDto dto) {
        LegalDoc doc = new LegalDoc();
        doc.setType(dto.type());
        doc.setVersion(dto.version());
        doc.setContent(dto.content());
        doc.setEffectiveAt(dto.effectiveAt());
        return doc;
    }
}

