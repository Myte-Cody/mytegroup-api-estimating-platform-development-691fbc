package com.mytegroup.api.dto.migrations;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalizeMigrationDto {
    
    private String migrationId;
    
    @NotBlank(message = "Organization ID is required")
    private String orgId;
    
    private Boolean confirmCutover;
    
    public void setMigrationId(String migrationId) {
        this.migrationId = migrationId != null ? migrationId.trim() : null;
    }
    
    public void setOrgId(String orgId) {
        this.orgId = orgId != null ? orgId.trim() : null;
    }
}
