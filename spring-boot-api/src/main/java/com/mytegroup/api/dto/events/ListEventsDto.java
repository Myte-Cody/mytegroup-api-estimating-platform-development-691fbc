package com.mytegroup.api.dto.events;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListEventsDto {
    
    private String entityType;
    
    private String entityId;
    
    private String actorId;
    
    private String action;
    
    private String eventType;
    
    private String cursor;
    
    private Boolean archived;
    
    private LocalDateTime from;
    
    private LocalDateTime to;
    
    @Min(value = 0, message = "Offset must be non-negative")
    private Integer offset;
    
    @Min(value = 1, message = "Page must be at least 1")
    private Integer page;
    
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must be at most 100")
    private Integer limit;
    
    private String orgId;
    
    private String sort;
    
    public void setEntityType(String entityType) {
        this.entityType = entityType != null ? entityType.trim() : null;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId != null ? entityId.trim() : null;
    }
    
    public void setActorId(String actorId) {
        this.actorId = actorId != null ? actorId.trim() : null;
    }
    
    public void setAction(String action) {
        this.action = action != null ? action.trim() : null;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType != null ? eventType.trim() : null;
    }
    
    public void setCursor(String cursor) {
        this.cursor = cursor != null ? cursor.trim() : null;
    }
    
    public void setOrgId(String orgId) {
        this.orgId = orgId != null ? orgId.trim() : null;
    }
    
    public void setSort(String sort) {
        this.sort = sort != null ? sort.trim().toLowerCase() : null;
    }
}
