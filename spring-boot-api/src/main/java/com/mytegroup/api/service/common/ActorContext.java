package com.mytegroup.api.service.common;

import com.mytegroup.api.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActorContext {
    private String userId;
    private String orgId;
    private Role role;
}

