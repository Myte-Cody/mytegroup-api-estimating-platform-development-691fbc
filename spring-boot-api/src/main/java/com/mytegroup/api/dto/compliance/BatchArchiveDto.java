package com.mytegroup.api.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchArchiveDto {
    
    @NotBlank(message = "Entity type is required")
    private String entityType;
    
    @NotNull(message = "Entity IDs are required")
    @NotEmpty(message = "At least one entity ID is required")
    private List<String> entityIds;
    
    private String orgId;
    
    private Boolean archive;
    
    public void setEntityType(String entityType) {
        this.entityType = entityType != null ? entityType.trim() : null;
    }
}
