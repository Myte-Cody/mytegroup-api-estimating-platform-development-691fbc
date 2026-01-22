package com.mytegroup.api.mapper.legal;

import com.mytegroup.api.dto.legal.CreateLegalDocDto;
import com.mytegroup.api.entity.legal.LegalDoc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LegalMapper.
 */
class LegalMapperTest {

    private LegalMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LegalMapper();
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("TERMS");
        dto.setVersion("1.0");
        dto.setTitle("Terms of Service");
        dto.setContent("Legal content here");
        dto.setEffectiveAt(LocalDateTime.now());

        // When
        LegalDoc doc = mapper.toEntity(dto);

        // Then
        assertThat(doc).isNotNull();
        assertThat(doc.getType()).isEqualTo("TERMS");
        assertThat(doc.getVersion()).isEqualTo("1.0");
        assertThat(doc.getContent()).isEqualTo("Legal content here");
        assertThat(doc.getEffectiveAt()).isNotNull();
    }
}

