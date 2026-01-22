package com.mytegroup.api.mapper.invites;

import com.mytegroup.api.dto.invites.CreateInviteDto;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.people.Person;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InviteMapper {
    public Invite toEntity(CreateInviteDto dto, com.mytegroup.api.entity.core.Organization organization, Person person) {
        Invite invite = new Invite();
        invite.setOrganization(organization);
        invite.setPerson(person);
        invite.setRole(dto.getRole());
        // Token hash and expires will be set by the service
        if (dto.getExpiresInHours() != null) {
            invite.setTokenExpires(LocalDateTime.now().plusHours(dto.getExpiresInHours()));
        } else {
            invite.setTokenExpires(LocalDateTime.now().plusDays(7)); // Default 7 days
        }
        return invite;
    }
}

