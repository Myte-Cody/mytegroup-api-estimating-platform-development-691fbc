package com.mytegroup.api.mapper.invites;

import com.mytegroup.api.dto.invites.CreateInviteDto;
import com.mytegroup.api.entity.core.Invite;
import com.mytegroup.api.entity.people.Person;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InviteMapper {
    public Invite toEntity(CreateInviteDto dto, Person person) {
        Invite invite = new Invite();
        invite.setPerson(person);
        invite.setRole(dto.role());
        if (dto.expiresInHours() != null) {
            invite.setExpiresAt(LocalDateTime.now().plusHours(dto.expiresInHours()));
        }
        return invite;
    }
}

