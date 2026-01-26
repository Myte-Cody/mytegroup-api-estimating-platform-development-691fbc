package com.mytegroup.api.mapper.legal;

import com.mytegroup.api.dto.legal.CreateLegalDocDto;
import com.mytegroup.api.entity.legal.LegalDoc;
import com.mytegroup.api.entity.enums.legal.LegalDocType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LegalMapperUnitTest {

    private LegalMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LegalMapper();
    }

    @Test
    void testCreateLegalDocDtoToEntity() {
        // Arrange
        LocalDateTime effectiveAt = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("TERMS");
        dto.setVersion("1.0");
        dto.setContent("Terms and conditions");
        dto.setEffectiveAt(effectiveAt);

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertNotNull(doc);
        assertEquals(LegalDocType.TERMS, doc.getType());
        assertEquals("1.0", doc.getVersion());
        assertEquals("Terms and conditions", doc.getContent());
        assertEquals(effectiveAt, doc.getEffectiveAt());
    }

    @Test
    void testCreateLegalDocDtoToEntityWithPrivacyPolicy() {
        // Arrange
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("PRIVACY_POLICY");
        dto.setVersion("2.0");
        dto.setContent("Privacy policy content");
        dto.setEffectiveAt(LocalDateTime.of(2024, 2, 1, 0, 0, 0));

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertEquals(LegalDocType.PRIVACY_POLICY, doc.getType());
        assertEquals("2.0", doc.getVersion());
    }

    @Test
    void testCreateLegalDocDtoToEntityWithNullType() {
        // Arrange
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType(null);
        dto.setVersion("2.0");
        dto.setContent("Content");
        dto.setEffectiveAt(LocalDateTime.now());

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertNull(doc.getType());
        assertEquals("2.0", doc.getVersion());
    }

    @Test
    void testCreateLegalDocDtoToEntityWithNullEffectiveAt() {
        // Arrange
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("PRIVACY_POLICY");
        dto.setVersion("1.0");
        dto.setContent("Privacy policy");
        dto.setEffectiveAt(null);

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertNotNull(doc.getEffectiveAt());
        // Should default to now
        LocalDateTime now = LocalDateTime.now();
        assertTrue(doc.getEffectiveAt().isAfter(now.minusMinutes(1)));
        assertTrue(doc.getEffectiveAt().isBefore(now.plusMinutes(1)));
    }

    @Test
    void testCreateLegalDocDtoToEntityWithLowercaseType() {
        // Arrange
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("privacy_policy"); // lowercase - will be converted to uppercase
        dto.setVersion("1.0");
        dto.setContent("Privacy");
        dto.setEffectiveAt(LocalDateTime.now());

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertEquals(LegalDocType.PRIVACY_POLICY, doc.getType());
    }

    @Test
    void testCreateLegalDocDtoToEntityWithDifferentLegalDocTypes() {
        // Arrange
        LegalDocType[] types = {
                LegalDocType.TERMS,
                LegalDocType.PRIVACY_POLICY
        };

        for (LegalDocType type : types) {
            CreateLegalDocDto dto = new CreateLegalDocDto();
            dto.setType(type.name());
            dto.setVersion("1.0");
            dto.setContent("Content");
            dto.setEffectiveAt(LocalDateTime.now());

            // Act
            LegalDoc doc = mapper.toEntity(dto);

            // Assert
            assertEquals(type, doc.getType());
        }
    }

    @Test
    void testCreateLegalDocDtoToEntityMapsAllFields() {
        // Arrange
        LocalDateTime effective = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("TERMS");
        dto.setVersion("3.5");
        dto.setContent("Comprehensive terms document");
        dto.setEffectiveAt(effective);

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertEquals(LegalDocType.TERMS, doc.getType());
        assertEquals("3.5", doc.getVersion());
        assertEquals("Comprehensive terms document", doc.getContent());
        assertEquals(effective, doc.getEffectiveAt());
    }

    @Test
    void testCreateLegalDocDtoToEntityPreservesVersion() {
        // Arrange
        String[] versions = {"1.0", "2.0", "2.1", "3.0-beta", "1.0.1"};

        for (String version : versions) {
            CreateLegalDocDto dto = new CreateLegalDocDto();
            dto.setType("TERMS");
            dto.setVersion(version);
            dto.setContent("Content");
            dto.setEffectiveAt(LocalDateTime.now());

            // Act
            LegalDoc doc = mapper.toEntity(dto);

            // Assert
            assertEquals(version, doc.getVersion());
        }
    }

    @Test
    void testCreateLegalDocDtoToEntityPreservesContent() {
        // Arrange
        String content = "This is a long legal document with multiple paragraphs\n\n" +
                "Second paragraph\n\n" +
                "Third paragraph with special characters: © ® ™ § ¶";

        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("PRIVACY_POLICY");
        dto.setVersion("1.0");
        dto.setContent(content);
        dto.setEffectiveAt(LocalDateTime.now());

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertEquals(content, doc.getContent());
    }

    @Test
    void testCreateLegalDocDtoToEntityWithNullVersion() {
        // Arrange
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("PRIVACY_POLICY");
        dto.setVersion(null);
        dto.setContent("Privacy");
        dto.setEffectiveAt(LocalDateTime.now());

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertNull(doc.getVersion());
    }

    @Test
    void testCreateLegalDocDtoToEntityWithNullContent() {
        // Arrange
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("TERMS");
        dto.setVersion("1.0");
        dto.setContent(null);
        dto.setEffectiveAt(LocalDateTime.now());

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertNull(doc.getContent());
    }

    @Test
    void testCreateLegalDocDtoToEntityWithEmptyStrings() {
        // Arrange
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("PRIVACY_POLICY");
        dto.setVersion("");
        dto.setContent("");
        dto.setEffectiveAt(LocalDateTime.now());

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertEquals("", doc.getVersion());
        assertEquals("", doc.getContent());
    }

    @Test
    void testCreateLegalDocDtoToEntityWithDifferentEffectiveAtDates() {
        // Arrange
        LocalDateTime[] dates = {
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 6, 15, 12, 30, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59, 59)
        };

        for (LocalDateTime date : dates) {
            CreateLegalDocDto dto = new CreateLegalDocDto();
            dto.setType("TERMS");
            dto.setVersion("1.0");
            dto.setContent("Content");
            dto.setEffectiveAt(date);

            // Act
            LegalDoc doc = mapper.toEntity(dto);

            // Assert
            assertEquals(date, doc.getEffectiveAt());
        }
    }

    @Test
    void testCreateLegalDocDtoToEntityWithMixedCaseType() {
        // Arrange
        CreateLegalDocDto dto = new CreateLegalDocDto();
        dto.setType("pRiVaCy_PoLiCy"); // Mixed case - will be converted to uppercase
        dto.setVersion("1.0");
        dto.setContent("Privacy");
        dto.setEffectiveAt(LocalDateTime.now());

        // Act
        LegalDoc doc = mapper.toEntity(dto);

        // Assert
        assertEquals(LegalDocType.PRIVACY_POLICY, doc.getType());
    }
}



