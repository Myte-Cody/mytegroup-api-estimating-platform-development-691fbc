package com.mytegroup.api.service.organizations;

import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.organization.DatastoreType;
import com.mytegroup.api.entity.enums.organization.DataResidency;
import com.mytegroup.api.exception.BadRequestException;
import com.mytegroup.api.exception.ConflictException;
import com.mytegroup.api.exception.ForbiddenException;
import com.mytegroup.api.exception.ResourceNotFoundException;
import com.mytegroup.api.repository.core.OrganizationRepository;
import com.mytegroup.api.repository.core.UserRepository;
import com.mytegroup.api.service.common.AuditLogService;
import com.mytegroup.api.service.common.ServiceAuthorizationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationsServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ServiceAuthorizationHelper authHelper;

    @InjectMocks
    private OrganizationsService organizationsService;

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");
        testOrganization.setPrimaryDomain("example.com");
        testOrganization.setDatastoreType(DatastoreType.SHARED);
        testOrganization.setDataResidency(DataResidency.SHARED);
        testOrganization.setMetadata(new HashMap<>());

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    void testFindByDomain_WithValidDomain_ReturnsOrganization() {
        String domain = "example.com";
        when(organizationRepository.findByPrimaryDomain(domain)).thenReturn(Optional.of(testOrganization));

        Organization result = organizationsService.findByDomain(domain);

        assertNotNull(result);
        assertEquals(domain, result.getPrimaryDomain());
    }

    @Test
    void testFindByDomain_WithArchivedOrganization_ReturnsNull() {
        String domain = "example.com";
        testOrganization.setArchivedAt(LocalDateTime.now());
        when(organizationRepository.findByPrimaryDomain(domain)).thenReturn(Optional.of(testOrganization));

        Organization result = organizationsService.findByDomain(domain);

        assertNull(result);
    }

    @Test
    void testFindByDomain_WithNullDomain_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            organizationsService.findByDomain(null);
        });
    }

    @Test
    void testFindByDomain_WithEmptyDomain_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> {
            organizationsService.findByDomain("   ");
        });
    }

    @Test
    void testCreate_WithValidOrganization_CreatesOrganization() {
        Organization newOrg = new Organization();
        newOrg.setName("New Org");
        newOrg.setPrimaryDomain("neworg.com");

        when(organizationRepository.findByName("New Org")).thenReturn(Optional.empty());
        when(organizationRepository.findByPrimaryDomain("neworg.com")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
            Organization org = invocation.getArgument(0);
            org.setId(1L);
            return org;
        });

        Organization result = organizationsService.create(newOrg);

        assertNotNull(result);
        assertEquals("New Org", result.getName());
        assertEquals("neworg.com", result.getPrimaryDomain());
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    void testCreate_WithDuplicateName_ThrowsConflictException() {
        Organization newOrg = new Organization();
        newOrg.setName("Existing Org");

        when(organizationRepository.findByName("Existing Org")).thenReturn(Optional.of(testOrganization));

        assertThrows(ConflictException.class, () -> {
            organizationsService.create(newOrg);
        });
    }

    @Test
    void testCreate_WithDuplicateDomain_ThrowsConflictException() {
        Organization newOrg = new Organization();
        newOrg.setName("New Org");
        newOrg.setPrimaryDomain("example.com");

        when(organizationRepository.findByName("New Org")).thenReturn(Optional.empty());
        when(organizationRepository.findByPrimaryDomain("example.com")).thenReturn(Optional.of(testOrganization));

        assertThrows(ConflictException.class, () -> {
            organizationsService.create(newOrg);
        });
    }

    @Test
    void testCreate_WithDedicatedDatastoreAndNoUri_ThrowsBadRequestException() {
        Organization newOrg = new Organization();
        newOrg.setName("New Org");
        newOrg.setDatastoreType(DatastoreType.DEDICATED);

        when(organizationRepository.findByName("New Org")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> {
            organizationsService.create(newOrg);
        });
    }

    @Test
    void testSetOwner_WithValidIds_SetsOwner() {
        Long orgId = 1L;
        Long userId = 1L;

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(authHelper).ensureNotOnLegalHold(any(Organization.class), anyString());

        Organization result = organizationsService.setOwner(orgId, userId);

        assertNotNull(result);
        assertEquals(testUser, result.getOwnerUser());
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    void testSetOwner_WithNonExistentOrg_ThrowsResourceNotFoundException() {
        Long orgId = 999L;
        Long userId = 1L;

        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            organizationsService.setOwner(orgId, userId);
        });
    }

    @Test
    void testSetOwner_WithNonExistentUser_ThrowsResourceNotFoundException() {
        Long orgId = 1L;
        Long userId = 999L;

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            organizationsService.setOwner(orgId, userId);
        });
    }

    @Test
    void testUpdate_WithValidUpdates_UpdatesOrganization() {
        Long orgId = 1L;
        Organization updates = new Organization();
        updates.setName("Updated Org Name");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.findByName("Updated Org Name")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(authHelper).ensureNotOnLegalHold(any(Organization.class), anyString());

        Organization result = organizationsService.update(orgId, updates);

        assertNotNull(result);
        assertEquals("Updated Org Name", result.getName());
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    void testUpdate_WithDuplicateName_ThrowsConflictException() {
        Long orgId = 1L;
        Organization updates = new Organization();
        updates.setName("Duplicate Name");

        Organization otherOrg = new Organization();
        otherOrg.setId(2L);
        otherOrg.setName("Duplicate Name");

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.findByName("Duplicate Name")).thenReturn(Optional.of(otherOrg));
        doNothing().when(authHelper).ensureNotOnLegalHold(any(Organization.class), anyString());

        assertThrows(ConflictException.class, () -> {
            organizationsService.update(orgId, updates);
        });
    }

    @Test
    void testUpdate_WithNonExistentOrg_ThrowsResourceNotFoundException() {
        Long orgId = 999L;
        Organization updates = new Organization();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            organizationsService.update(orgId, updates);
        });
    }

    @Test
    void testFindById_WithValidId_ReturnsOrganization() {
        Long orgId = 1L;
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));

        Organization result = organizationsService.findById(orgId);

        assertNotNull(result);
        assertEquals(orgId, result.getId());
    }

    @Test
    void testFindById_WithNonExistentId_ThrowsResourceNotFoundException() {
        Long orgId = 999L;
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            organizationsService.findById(orgId);
        });
    }

    @Test
    void testList_WithValidParams_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Organization> emptyPage = Page.empty(pageable);
        when(organizationRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        Page<Organization> result = organizationsService.list(null, null, null, null, null, 0, 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(organizationRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSetLegalHold_WithValidOrg_SetsLegalHold() {
        Long orgId = 1L;
        Boolean legalHold = true;

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Organization result = organizationsService.setLegalHold(orgId, legalHold);

        assertNotNull(result);
        assertEquals(legalHold, result.getLegalHold());
        verify(organizationRepository, times(1)).save(any(Organization.class));
    }

    @Test
    void testSetLegalHold_WithSameValue_ReturnsWithoutSaving() {
        Long orgId = 1L;
        testOrganization.setLegalHold(true);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));

        Organization result = organizationsService.setLegalHold(orgId, true);

        assertNotNull(result);
        verify(organizationRepository, never()).save(any(Organization.class));
    }
}

