package com.mytegroup.api.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetLegalHoldDto {
    
    @NotBlank(message = "Entity type is required")
    private String entityType;
    
    @NotBlank(message = "Entity ID is required")
    private String entityId;
    
    @NotNull(message = "Legal hold status is required")
    private Boolean legalHold;
    
    private String orgId;
    
    private String reason;
    
    public void setEntityType(String entityType) {
        this.entityType = entityType != null ? entityType.trim() : null;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId != null ? entityId.trim() : null;
    }
    
    public void setReason(String reason) {
        this.reason = reason != null ? reason.trim() : null;
    }
}
