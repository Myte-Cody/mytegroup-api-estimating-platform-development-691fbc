package com.mytegroup.api.dto.waitlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEventDto {
    
    @NotBlank(message = "Event is required")
    @Size(max = 100, message = "Event must be at most 100 characters")
    private String event;
    
    private Map<String, Object> meta;
    
    @Size(max = 255, message = "Source must be at most 255 characters")
    private String source;
    
    @Size(max = 500, message = "Path must be at most 500 characters")
    private String path;
    
    public void setEvent(String event) {
        this.event = event != null ? event.trim() : null;
    }
    
    public void setSource(String source) {
        this.source = source != null ? source.trim() : null;
    }
    
    public void setPath(String path) {
        this.path = path != null ? path.trim() : null;
    }
}
