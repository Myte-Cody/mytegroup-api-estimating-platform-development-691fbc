package com.mytegroup.api.mapper.response;

import com.mytegroup.api.dto.response.ProjectResponseDto;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.companies.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ProjectResponseMapperUnitTest {

    private ProjectResponseMapper projectResponseMapper;

    @BeforeEach
    void setUp() {
        projectResponseMapper = new ProjectResponseMapper();
    }

    @Test
    void testProjectEntityToResponseDto() {
        // Arrange
        Organization org = new Organization();
        org.setId(1L);

        Company company = new Company();
        company.setId(5L);

        Office office = new Office();
        office.setId(3L);

        Project project = new Project();
        project.setId(10L);
        project.setName("Office Building");
        project.setDescription("New office building project");
        project.setStatus("Active");
        project.setProjectManager("John Smith");
        project.setIsActive(true);
        project.setCompany(company);
        project.setOffice(office);
        project.setOrganization(org);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        // Act
        ProjectResponseDto dto = projectResponseMapper.toDto(project);

        // Assert
        assertNotNull(dto);
        assertEquals(10L, dto.id());
        assertEquals("Office Building", dto.name());
        assertEquals("New office building project", dto.description());
        assertEquals("Active", dto.status());
        assertEquals("John Smith", dto.projectManager());
        assertTrue(dto.isActive());
        assertEquals("5", dto.companyId());
        assertEquals("3", dto.officeId());
        assertEquals(1L, dto.orgId());
    }

    @Test
    void testProjectWithoutCompanyAndOffice() {
        // Arrange
        Organization org = new Organization();
        org.setId(2L);

        Project project = new Project();
        project.setId(20L);
        project.setName("Simple Project");
        project.setDescription("Simple project description");
        project.setCompany(null);
        project.setOffice(null);
        project.setOrganization(org);

        // Act
        ProjectResponseDto dto = projectResponseMapper.toDto(project);

        // Assert
        assertNotNull(dto);
        assertEquals(20L, dto.id());
        assertEquals("Simple Project", dto.name());
        assertNull(dto.companyId());
        assertNull(dto.officeId());
    }

    @Test
    void testNullEntityReturnsNull() {
        // Act
        ProjectResponseDto dto = projectResponseMapper.toDto(null);

        // Assert
        assertNull(dto);
    }
}

