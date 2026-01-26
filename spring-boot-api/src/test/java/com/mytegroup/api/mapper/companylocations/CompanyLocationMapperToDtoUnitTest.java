package com.mytegroup.api.mapper.companylocations;

import com.mytegroup.api.dto.response.CompanyLocationResponseDto;
import com.mytegroup.api.entity.companies.Company;
import com.mytegroup.api.entity.companies.CompanyLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CompanyLocationMapperToDtoUnitTest {

    private CompanyLocationMapper mapper;
    private Company company;

    @BeforeEach
    void setUp() {
        mapper = new CompanyLocationMapper();
        company = new Company();
        company.setId(1L);
    }

    @Test
    void testToDtoWithFullEntity() {
        // Arrange
        CompanyLocation entity = new CompanyLocation();
        entity.setId(10L);
        entity.setCompany(company);
        entity.setAddressLine1("123 Main St");
        entity.setCity("New York");
        entity.setRegion("NY");
        entity.setPostal("10001");
        entity.setCountry("USA");
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 11, 0, 0));

        // Act
        CompanyLocationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals(1L, dto.getCompanyId());
        assertEquals("123 Main St", dto.getAddress());
        assertEquals("New York", dto.getCity());
        assertEquals("NY", dto.getRegion());
        assertEquals("10001", dto.getPostal());
        assertEquals("USA", dto.getCountry());
    }

    @Test
    void testToDtoNullEntity() {
        // Act
        CompanyLocationResponseDto dto = mapper.toDto(null);

        // Assert
        assertNull(dto);
    }

    @Test
    void testToDtoWithoutCompany() {
        // Arrange
        CompanyLocation entity = new CompanyLocation();
        entity.setId(11L);
        entity.setCompany(null);
        entity.setAddressLine1("Address");
        entity.setCity("City");

        // Act
        CompanyLocationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertNull(dto.getCompanyId());
        assertEquals("Address", dto.getAddress());
    }

    @Test
    void testToDoBuildsMapsAllFields() {
        // Arrange
        CompanyLocation entity = new CompanyLocation();
        entity.setId(12L);
        entity.setCompany(company);
        entity.setAddressLine1("456 Park Ave");
        entity.setCity("Boston");
        entity.setRegion("MA");
        entity.setPostal("02101");
        entity.setCountry("USA");
        entity.setArchivedAt(LocalDateTime.of(2024, 6, 1, 0, 0, 0));
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 11, 30, 0));

        // Act
        CompanyLocationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("456 Park Ave", dto.getAddress());
        assertEquals("Boston", dto.getCity());
        assertEquals("MA", dto.getRegion());
        assertEquals("02101", dto.getPostal());
        assertEquals("USA", dto.getCountry());
        assertNotNull(dto.getArchivedAt());
    }

    @Test
    void testToDoBuildsMapsWithoutCompanyLocation() {
        // Arrange
        CompanyLocation entity = new CompanyLocation();
        entity.setId(13L);
        entity.setCompany(company);
        entity.setAddressLine1("789 Ave");
        entity.setCity(null);
        entity.setRegion(null);
        entity.setPostal(null);
        entity.setCountry(null);

        // Act
        CompanyLocationResponseDto dto = mapper.toDto(entity);

        // Assert
        assertEquals("789 Ave", dto.getAddress());
        assertNull(dto.getCity());
        assertNull(dto.getRegion());
    }
}



