package com.mytegroup.api.dto.graphedges;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGraphEdgeDto {
    
    @NotBlank(message = "From type is required")
    private String fromType;
    
    @NotBlank(message = "From ID is required")
    private String fromId;
    
    @NotBlank(message = "To type is required")
    private String toType;
    
    @NotBlank(message = "To ID is required")
    private String toId;
    
    @NotBlank(message = "Edge type is required")
    private String edgeType;
    
    private Map<String, Object> meta;
    
    private LocalDate effectiveFrom;
    
    private LocalDate effectiveTo;
    
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
