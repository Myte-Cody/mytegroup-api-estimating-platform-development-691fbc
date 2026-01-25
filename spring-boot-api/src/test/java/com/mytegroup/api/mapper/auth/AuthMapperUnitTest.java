package com.mytegroup.api.mapper.auth;

import com.mytegroup.api.dto.auth.LoginDto;
import com.mytegroup.api.dto.auth.RegisterDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthMapperUnitTest {

    private AuthMapper mapper;
    private Organization organization;

    @BeforeEach
    void setUp() {
        mapper = new AuthMapper();
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Organization");
    }

    @Test
    void testRegisterDtoToEntity() {
        // Arrange
        RegisterDto dto = new RegisterDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setUsername("johndoe");
        dto.setEmail("john@example.com");
        dto.setRole(Role.USER);

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(user);
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("johndoe", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
        assertEquals(organization, user.getOrganization());
        assertEquals(Role.USER, user.getRole());
        assertNotNull(user.getRoles());
        assertEquals(1, user.getRoles().size());
        assertEquals(Role.USER, user.getRoles().get(0));
        assertFalse(user.getIsEmailVerified());
        assertFalse(user.getIsOrgOwner());
        assertFalse(user.getPiiStripped());
        assertFalse(user.getLegalHold());
    }

    @Test
    void testRegisterDtoToEntityWithNullRole() {
        // Arrange
        RegisterDto dto = new RegisterDto();
        dto.setFirstName("Jane");
        dto.setLastName("Smith");
        dto.setUsername("janesmith");
        dto.setEmail("jane@example.com");
        dto.setRole(null);

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertNotNull(user);
        assertEquals(Role.USER, user.getRole());
        assertNotNull(user.getRoles());
        assertEquals(1, user.getRoles().size());
        assertEquals(Role.USER, user.getRoles().get(0));
    }

    @Test
    void testRegisterDtoToEntityWithAdminRole() {
        // Arrange
        RegisterDto dto = new RegisterDto();
        dto.setFirstName("Admin");
        dto.setLastName("User");
        dto.setUsername("admin");
        dto.setEmail("admin@example.com");
        dto.setRole(Role.ADMIN);

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertEquals(Role.ADMIN, user.getRole());
        assertNotNull(user.getRoles());
        assertEquals(1, user.getRoles().size());
        assertEquals(Role.ADMIN, user.getRoles().get(0));
    }

    @Test
    void testRegisterDtoToEntitySetsDefaultFlags() {
        // Arrange
        RegisterDto dto = new RegisterDto();
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertFalse(user.getIsEmailVerified());
        assertFalse(user.getIsOrgOwner());
        assertFalse(user.getPiiStripped());
        assertFalse(user.getLegalHold());
    }

    @Test
    void testLoginDtoToLoginCredentials() {
        // Arrange
        LoginDto dto = new LoginDto();
        dto.setEmail("user@example.com");
        dto.setPassword("securePassword123");

        // Act
        AuthMapper.LoginCredentials credentials = mapper.toLoginCredentials(dto);

        // Assert
        assertNotNull(credentials);
        assertEquals("user@example.com", credentials.email());
        assertEquals("securePassword123", credentials.password());
    }

    @Test
    void testLoginDtoToLoginCredentialsWithNullEmail() {
        // Arrange
        LoginDto dto = new LoginDto();
        dto.setEmail(null);
        dto.setPassword("password");

        // Act
        AuthMapper.LoginCredentials credentials = mapper.toLoginCredentials(dto);

        // Assert
        assertNull(credentials.email());
        assertEquals("password", credentials.password());
    }

    @Test
    void testLoginDtoToLoginCredentialsWithNullPassword() {
        // Arrange
        LoginDto dto = new LoginDto();
        dto.setEmail("user@example.com");
        dto.setPassword(null);

        // Act
        AuthMapper.LoginCredentials credentials = mapper.toLoginCredentials(dto);

        // Assert
        assertEquals("user@example.com", credentials.email());
        assertNull(credentials.password());
    }

    @Test
    void testRegisterDtoToEntityWithDifferentRoles() {
        // Arrange
        Role[] roles = {Role.ADMIN, Role.USER, Role.VIEWER};

        for (Role role : roles) {
            RegisterDto dto = new RegisterDto();
            dto.setFirstName("Test");
            dto.setLastName("User");
            dto.setUsername("testuser");
            dto.setEmail("test@example.com");
            dto.setRole(role);

            // Act
            User user = mapper.toEntity(dto, organization);

            // Assert
            assertEquals(role, user.getRole());
            assertEquals(role, user.getRoles().get(0));
        }
    }

    @Test
    void testRegisterDtoToEntityMapsAllFields() {
        // Arrange
        RegisterDto dto = new RegisterDto();
        dto.setFirstName("Complete");
        dto.setLastName("User");
        dto.setUsername("completeuser");
        dto.setEmail("complete@example.com");
        dto.setRole(Role.ADMIN);

        // Act
        User user = mapper.toEntity(dto, organization);

        // Assert
        assertEquals("Complete", user.getFirstName());
        assertEquals("User", user.getLastName());
        assertEquals("completeuser", user.getUsername());
        assertEquals("complete@example.com", user.getEmail());
        assertEquals(organization, user.getOrganization());
        assertEquals(Role.ADMIN, user.getRole());
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().contains(Role.ADMIN));
    }

    @Test
    void testRegisterDtoToEntityPreservesOrganization() {
        // Arrange
        Organization org2 = new Organization();
        org2.setId(42L);
        org2.setName("Other Organization");

        RegisterDto dto = new RegisterDto();
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");

        // Act
        User user = mapper.toEntity(dto, org2);

        // Assert
        assertEquals(org2, user.getOrganization());
        assertEquals(42L, org2.getId());
    }

    @Test
    void testLoginCredentialsRecord() {
        // Arrange
        String email = "test@example.com";
        String password = "TestPassword123!";

        // Act
        AuthMapper.LoginCredentials creds = new AuthMapper.LoginCredentials(email, password);

        // Assert
        assertEquals(email, creds.email());
        assertEquals(password, creds.password());
    }

    @Test
    void testLoginDtoToLoginCredentialsPreservesValues() {
        // Arrange
        String[] emails = {"simple@test.com", "with+plus@domain.com", "under_score@test.co.uk"};
        String[] passwords = {"password", "P@ssw0rd!", "VeryLongPasswordWith123SpecialChars!@#"};

        for (String email : emails) {
            for (String password : passwords) {
                LoginDto dto = new LoginDto();
                dto.setEmail(email);
                dto.setPassword(password);

                // Act
                AuthMapper.LoginCredentials creds = mapper.toLoginCredentials(dto);

                // Assert
                assertEquals(email, creds.email());
                assertEquals(password, creds.password());
            }
        }
    }
}
