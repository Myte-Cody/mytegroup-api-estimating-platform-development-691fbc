package com.mytegroup.api.mapper.invites;

import com.mytegroup.api.common.enums.Role;
import com.mytegroup.api.dto.invites.CreateInviteDto;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.people.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InviteMapper.
 */
class InviteMapperTest {

    private InviteMapper mapper;
    private Person testPerson;

    @BeforeEach
    void setUp() {
        mapper = new InviteMapper();
        testPerson = new Person();
        testPerson.setId(1L);
    }

    @Test
    void shouldMapCreateDtoToEntity() {
        // Given
        CreateInviteDto dto = new CreateInviteDto();
        dto.setPersonId("1");
        dto.setRole(Role.ADMIN);
        dto.setExpiresInHours(24);

        // When
        Invite invite = mapper.toEntity(dto, testPerson);

        // Then
        assertThat(invite).isNotNull();
        assertThat(invite.getPerson()).isEqualTo(testPerson);
        assertThat(invite.getRole()).isEqualTo(Role.ADMIN);
        assertThat(invite.getExpiresAt()).isNotNull();
        assertThat(invite.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void shouldMapCreateDtoWithNullExpiresInHours() {
        // Given
        CreateInviteDto dto = new CreateInviteDto();
        dto.setPersonId("1");
        dto.setRole(Role.USER);
        dto.setExpiresInHours(null);

        // When
        Invite invite = mapper.toEntity(dto, testPerson);

        // Then
        assertThat(invite.getExpiresAt()).isNull();
    }
}

