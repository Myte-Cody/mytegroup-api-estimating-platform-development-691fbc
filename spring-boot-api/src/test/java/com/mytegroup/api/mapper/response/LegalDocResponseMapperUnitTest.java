package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.LegalDocResponseDto;
import com.mytegroup.api.entity.enums.legal.LegalDocType;
import com.mytegroup.api.entity.legal.LegalDoc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LegalDocResponseMapperUnitTest {

    private LegalDocResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LegalDocResponseMapper();
    }

    @Test
    void testToDotFullEntity() {
        // Arrange
        LegalDoc entity = new LegalDoc();
        entity.setId(10L);
        entity.setType(LegalDocType.TERMS);
        entity.setVersion("1.0");
        entity.setContent("Terms and conditions");
        entity.setEffectiveAt(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        LegalDocResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("terms", dto.getType());
        assertEquals("1.0", dto.getVersion());
        assertEquals("Terms and conditions", dto.getContent());
    }

    @Test
    void testToDotNullEntity() {
        // Act
        LegalDocResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDoBuildsMapsPrivacyPolicy() {
        // Arrange
        LegalDoc entity = new LegalDoc();
        entity.setId(11L);
        entity.setType(LegalDocType.PRIVACY_POLICY);
        entity.setVersion("2.0");
        entity.setContent("Privacy policy content");

        // Act
        LegalDocResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("privacy_policy", dto.getType());
        assertEquals("2.0", dto.getVersion());
    }

    @Test
    void testToDoBuildsMapsWithNullFields() {
        // Arrange
        LegalDoc entity = new LegalDoc();
        entity.setId(12L);
        entity.setType(null);
        entity.setVersion(null);
        entity.setContent(null);

        // Act
        LegalDocResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getType());
        assertNull(dto.getVersion());
        assertNull(dto.getContent());
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        LegalDoc entity = new LegalDoc();
        entity.setId(13L);
        entity.setType(LegalDocType.TERMS);
        entity.setVersion("3.5");
        entity.setContent("Comprehensive terms document with all clauses");
        entity.setEffectiveAt(LocalDateTime.of(2024, 6, 1, 12, 0, 0));
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 0, 0));

        // Act
        LegalDocResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("terms", dto.getType());
        assertEquals("3.5", dto.getVersion());
        assertEquals("Comprehensive terms document with all clauses", dto.getContent());
        assertNotNull(dto.getEffectiveAt());
    }

    @Test
    void testToDoBuildsMapsDateFields() {
        // Arrange
        LocalDateTime created = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        LocalDateTime updated = LocalDateTime.of(2024, 1, 15, 11, 30, 0);
        LocalDateTime effective = LocalDateTime.of(2024, 6, 1, 0, 0, 0);

        LegalDoc entity = new LegalDoc();
        entity.setId(14L);
        entity.setType(LegalDocType.PRIVACY_POLICY);
        entity.setVersion("1.0");
        entity.setContent("Privacy");
        entity.setEffectiveAt(effective);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);

        // Act
        LegalDocResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals(created, dto.getCreatedAt());
        assertEquals(updated, dto.getUpdatedAt());
        assertEquals(effective, dto.getEffectiveAt());
    }
}


