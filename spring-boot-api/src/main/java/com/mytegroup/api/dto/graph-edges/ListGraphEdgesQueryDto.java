package com.mytegroup.api.dto.graphedges;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListGraphEdgesQueryDto {
    
    private String orgId;
    
    private Boolean includeArchived;
    
    private String fromType;
    
    private String fromId;
    
    private String toType;
    
    private String toId;
    
    private String edgeType;
    
    @Min(value = 1, message = "Page must be at least 1")
    private Integer page;
    
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    private Integer limit;
    
    public void setOrgId(String orgId) {
        this.orgId = orgId != null ? orgId.trim() : null;
    }
    
    public void setFromId(String fromId) {
        this.fromId = fromId != null ? fromId.trim() : null;
    }
    
    public void setToId(String toId) {
        this.toId = toId != null ? toId.trim() : null;
    }
    
    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType != null ? edgeType.trim().toLowerCase().replaceAll("[^a-z0-9_]", "_") : null;
    }
}
