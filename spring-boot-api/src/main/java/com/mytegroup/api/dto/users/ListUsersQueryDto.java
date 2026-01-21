package com.mytegroup.api.dto.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListUsersQueryDto {
    
    private Boolean includeArchived;
    
    private String orgId;
    
    public void setOrgId(String orgId) {
        this.orgId = orgId != null ? orgId.trim() : null;
    }
}
