package com.mytegroup.api.dto.crmcontext;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListCrmContextDocumentsQueryDto {
    
    private String orgId;
    
    private String entityType;
    
    private String entityId;
    
    private Boolean includeArchived;
    
    @Min(value = 1, message = "Page must be at least 1")
    private Integer page;
    
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 250, message = "Limit must be at most 250")
    private Integer limit;
    
    public void setOrgId(String orgId) {
        this.orgId = orgId != null ? orgId.trim() : null;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType != null ? entityType.trim() : null;
    }
}
