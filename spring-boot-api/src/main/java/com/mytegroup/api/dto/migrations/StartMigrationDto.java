package com.mytegroup.api.dto.migrations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartMigrationDto {
    
    @NotBlank(message = "Organization ID is required")
    private String orgId;
    
    private String targetDatastoreType;
    
    private String targetUri;
    
    private String targetDbName;
    
    private Boolean dryRun;
    
    private Boolean resume;
    
    private Boolean overrideLegalHold;
    
    @Min(value = 1, message = "Chunk size must be at least 1")
    @Max(value = 5000, message = "Chunk size must be at most 5000")
    private Integer chunkSize;
    
    public void setOrgId(String orgId) {
        this.orgId = orgId != null ? orgId.trim() : null;
    }
    
    public void setTargetUri(String targetUri) {
        this.targetUri = targetUri != null ? targetUri.trim() : null;
    }
    
    public void setTargetDbName(String targetDbName) {
        this.targetDbName = targetDbName != null ? targetDbName.trim() : null;
    }
}
