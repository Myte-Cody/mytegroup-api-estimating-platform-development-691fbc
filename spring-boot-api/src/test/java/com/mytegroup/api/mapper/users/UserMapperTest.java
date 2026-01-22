package com.mytegroup.api.mapper.users;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.users.CreateUserDto;
import com.mytegroup.api.dto.users.UpdateUserDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserMapper.
 */
class UserMapperTest {

    private UserMapper mapper;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
        testOrg = new Organization();
        testOrg.setId(1L);
        testOrg.setName("Test Org");
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreateUserDto dto = new CreateUserDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setRole(Role.ADMIN);
        dto.setRoles(List.of(Role.ADMIN, Role.ORG_ADMIN));
        dto.setVerificationTokenHash("tokenHash");
        dto.setVerificationTokenExpires(LocalDateTime.now().plusHours(24));
        dto.setIsEmailVerified(true);
        dto.setIsOrgOwner(false);

        // When
        User user = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getRoles()).containsExactly(Role.ADMIN, Role.ORG_ADMIN);
        assertThat(user.getOrganization()).isEqualTo(testOrg);
    }

    @Test
    void shouldMapCreateDtoWithNullRole() {
        // Given
        CreateUserDto dto = new CreateUserDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setRole(null);
        dto.setRoles(null);

        // When
        User user = mapper.toEntity(dto, testOrg);

        // Then
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getRoles()).containsExactly(Role.USER);
    }

    @Test
    void shouldUpdateEntityWithNonNullValues() {
        // Given
        User user = new User();
        user.setUsername("original");
        user.setFirstName("Original");
        user.setEmail("original@test.com");

        UpdateUserDto dto = new UpdateUserDto();
        dto.setUsername("updated");
        dto.setFirstName("Updated");
        dto.setLastName("Updated");
        dto.setEmail("updated@test.com");
        dto.setIsEmailVerified(true);
        dto.setPiiStripped(false);
        dto.setLegalHold(false);

        // When
        mapper.updateEntity(user, dto);

        // Then
        assertThat(user.getUsername()).isEqualTo("updated");
        assertThat(user.getFirstName()).isEqualTo("Updated");
        assertThat(user.getEmail()).isEqualTo("updated@test.com");
        assertThat(user.getIsEmailVerified()).isTrue();
    }

    @Test
    void shouldNotUpdateEntityWithNullValues() {
        // Given
        User user = new User();
        user.setUsername("original");
        user.setFirstName("Original");
        user.setEmail("original@test.com");

        UpdateUserDto dto = new UpdateUserDto();
        dto.setUsername(null);
        dto.setFirstName(null);
        dto.setLastName(null);
        dto.setEmail(null);
        dto.setIsEmailVerified(null);
        dto.setPiiStripped(null);
        dto.setLegalHold(null);

        // When
        mapper.updateEntity(user, dto);

        // Then
        assertThat(user.getUsername()).isEqualTo("original");
        assertThat(user.getFirstName()).isEqualTo("Original");
        assertThat(user.getEmail()).isEqualTo("original@test.com");
    }
}

