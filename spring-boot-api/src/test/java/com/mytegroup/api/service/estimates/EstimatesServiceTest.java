package com.mytegroup.api.service.estimates;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.repository.projects.EstimateRepository;
import com.mytegroup.api.repository.projects.ProjectRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstimatesServiceTest {

    @Mock
    private EstimateRepository estimateRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private EstimatesService estimatesService;

    private Organization testOrganization;
    private Project testProject;
    private Estimate testEstimate;
    private User testUser;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");
        testProject.setOrganization(testOrganization);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setOrganization(testOrganization);

        testEstimate = new Estimate();
        testEstimate.setId(1L);
        testEstimate.setName("Test Estimate");
        testEstimate.setProject(testProject);
        testEstimate.setOrganization(testOrganization);
        testEstimate.setCreatedByUser(testUser);
    }

    @Test
    void testCreate_WithValidEstimate_CreatesEstimate() {
        Estimate newEstimate = new Estimate();
        newEstimate.setName("New Estimate");
        Long projectId = 1L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(estimateRepository.findByOrganization_IdAndProjectIdAndName(1L, projectId, "New Estimate"))
            .thenReturn(Optional.empty());
        when(estimateRepository.save(any(Estimate.class))).thenAnswer(invocation -> {
            Estimate estimate = invocation.getArgument(0);
            estimate.setId(1L);
            return estimate;
        });

        // Mock security context
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("testuser");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Estimate result = estimatesService.create(newEstimate, projectId, orgId);

        assertNotNull(result);
        assertEquals(testProject, result.getProject());
        verify(estimateRepository, times(1)).save(any(Estimate.class));
    }

    @Test
    void testCreate_WithNullProjectId_ThrowsBadRequestException() {
        Estimate newEstimate = new Estimate();

        assertThrows(BadRequestException.class, () -> {
            estimatesService.create(newEstimate, null, "1");
        });
    }

    @Test
    void testCreate_WithNullOrgId_ThrowsBadRequestException() {
        Estimate newEstimate = new Estimate();

        assertThrows(BadRequestException.class, () -> {
            estimatesService.create(newEstimate, 1L, null);
        });
    }

    @Test
    void testCreate_WithEmptyName_ThrowsBadRequestException() {
        Estimate newEstimate = new Estimate();
        newEstimate.setName("   ");
        Long projectId = 1L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));

        assertThrows(BadRequestException.class, () -> {
            estimatesService.create(newEstimate, projectId, orgId);
        });
    }

    @Test
    void testCreate_WithNonExistentProject_ThrowsResourceNotFoundException() {
        Estimate newEstimate = new Estimate();
        newEstimate.setName("New Estimate");
        Long projectId = 999L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            estimatesService.create(newEstimate, projectId, orgId);
        });
    }

    @Test
    void testCreate_WithDuplicateName_ThrowsConflictException() {
        Estimate newEstimate = new Estimate();
        newEstimate.setName("Test Estimate");
        Long projectId = 1L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(testProject));
        when(estimateRepository.findByOrganization_IdAndProjectIdAndName(1L, projectId, "Test Estimate"))
            .thenReturn(Optional.of(testEstimate));

        assertThrows(ConflictException.class, () -> {
            estimatesService.create(newEstimate, projectId, orgId);
        });
    }

    @Test
    void testListByProject_WithValidParams_ReturnsList() {
        Long projectId = 1L;
        String orgId = "1";
        Boolean includeArchived = false;

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(estimateRepository.findByOrganization_IdAndProjectIdAndArchivedAtIsNull(1L, projectId))
            .thenReturn(List.of(testEstimate));

        List<Estimate> result = estimatesService.listByProject(projectId, orgId, includeArchived);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testListByProject_WithNullOrgId_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            estimatesService.listByProject(1L, null, false);
        });
    }

    @Test
    void testGetById_WithValidId_ReturnsEstimate() {
        Long estimateId = 1L;
        Long projectId = 1L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(estimateRepository.findById(estimateId)).thenReturn(Optional.of(testEstimate));

        Estimate result = estimatesService.getById(estimateId, projectId, orgId, false);

        assertNotNull(result);
        assertEquals(estimateId, result.getId());
    }

    @Test
    void testGetById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long estimateId = 999L;
        Long projectId = 1L;
        String orgId = "1";

        when(authHelper.validateOrg(orgId)).thenReturn(testOrganization);
        when(estimateRepository.findById(estimateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            estimatesService.getById(estimateId, projectId, orgId, false);
        });
    }
}

