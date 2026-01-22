package com.mytegroup.api.mapper.auth;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.auth.LoginDto;
import com.mytegroup.api.dto.auth.RegisterDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthMapper.
 */
class AuthMapperTest {

    private AuthMapper mapper;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        mapper = new AuthMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
        testOrg.setName("Test Org");
    }

    @Test
    void shouldMapRegisterDtoToEntity() {
        // Given
        RegisterDto dto = new RegisterDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPassword("password");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setRole(Role.USER);

        // When
        User user = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getOrganization()).isEqualTo(testOrg);
        assertThat(user.getIsEmailVerified()).isFalse();
        assertThat(user.getIsOrgOwner()).isFalse();
    }

    @Test
    void shouldMapRegisterDtoWithNullRole() {
        // Given
        RegisterDto dto = new RegisterDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPassword("password");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setRole(null);

        // When
        User user = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldMapLoginDtoToCredentials() {
        // Given
        LoginDto dto = new LoginDto("test@example.com", "password");

        // When
        AuthMapper.LoginCredentials credentials = mapper.toLoginCredentials(dto);

        // Then
        assertThat(credentials.email()).isEqualTo("test@example.com");
        assertThat(credentials.password()).isEqualTo("password");
    }
}

