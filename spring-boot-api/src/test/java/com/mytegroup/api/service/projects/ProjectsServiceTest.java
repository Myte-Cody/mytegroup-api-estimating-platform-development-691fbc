package com.mytegroup.api.service.projects;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.organization.Office;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.organization.OfficeRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import com.mytegroup.api.service.common.ServiceValidationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectsServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @Mock
    private ServiceValidationHelper validationHelper;

    @InjectMocks
    private ProjectsService projectsService;

    private Organization testOrganization;
    private Project testProject;
    private Office testOffice;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testOffice = new Office();
        testOffice.setId(1L);
        testOffice.setName("Test Office");
        testOffice.setOrganization(testOrganization);

        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setProjectCode("TP001");
        testProject.setOrganization(testOrganization);
    }

    @Test
    void testCreate_WithValidProject_CreatesProject() {
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setProjectCode("NP001");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(projectRepository.findByOrganization_IdAndName(1L, "New Project"))
            .thenReturn(Optional.empty());
        when(projectRepository.findByOrganization_IdAndProjectCode(1L, "NP001"))
            .thenReturn(Optional.empty());
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1L);
            return project;
        });

        Project result = projectsService.create(newProject, "1");

        assertNotNull(result);
        assertEquals(testOrganization, result.getOrganization());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        Project newProject = new Project();

        assertThrows(BadRequestException.class, () -> {
            projectsService.create(newProject, null);
        });
    }

    @Test
    void testCreate_WithEmptyName_ThrowsBadRequestException() {
        Project newProject = new Project();
        newProject.setName("   ");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);

        assertThrows(BadRequestException.class, () -> {
            projectsService.create(newProject, "1");
        });
    }

    @Test
    void testCreate_WithDuplicateName_ThrowsConflictException() {
        Project newProject = new Project();
        newProject.setName("Test Project");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(projectRepository.findByOrganization_IdAndName(1L, "Test Project"))
            .thenReturn(Optional.of(testProject));

        assertThrows(ConflictException.class, () -> {
            projectsService.create(newProject, "1");
        });
    }

    @Test
    void testCreate_WithDuplicateProjectCode_ThrowsConflictException() {
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setProjectCode("TP001");

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(projectRepository.findByOrganization_IdAndName(1L, "New Project"))
            .thenReturn(Optional.empty());
        when(projectRepository.findByOrganization_IdAndProjectCode(1L, "TP001"))
            .thenReturn(Optional.of(testProject));

        assertThrows(ConflictException.class, () -> {
            projectsService.create(newProject, "1");
        });
    }

    @Test
    void testCreate_WithValidOffice_SetsOffice() {
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setOffice(testOffice);

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(officeRepository.findById(1L)).thenReturn(Optional.of(testOffice));
        when(projectRepository.findByOrganization_IdAndName(1L, "New Project"))
            .thenReturn(Optional.empty());
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(1L);
            return project;
        });

        Project result = projectsService.create(newProject, "1");

        assertNotNull(result);
        assertEquals(testOffice, result.getOffice());
    }

    @Test
    void testCreate_WithNonExistentOffice_ThrowsResourceNotFoundException() {
        Project newProject = new Project();
        newProject.setName("New Project");
        newProject.setOffice(testOffice);

        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(officeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectsService.create(newProject, "1");
        });
    }

    @Test
    void testList_WithValidParams_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(authHelper.validateOrg("1")).thenReturn(testOrganization);
        when(projectRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(Page.empty());

        Page<Project> result = projectsService.list("1", null, null, false, 0, 10);

        assertNotNull(result);
        verify(projectRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testList_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            projectsService.list(null, null, null, false, 0, 10);
        });
    }

    @Test
    void testGetById_WithValidId_ReturnsProject() {
        Long projectId = 1L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

        Project result = projectsService.getById(projectId, "1", false);

        assertNotNull(result);
        assertEquals(projectId, result.getId());
    }

    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long projectId = 999L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectsService.getById(projectId, "1", false);
        });
    }
}



