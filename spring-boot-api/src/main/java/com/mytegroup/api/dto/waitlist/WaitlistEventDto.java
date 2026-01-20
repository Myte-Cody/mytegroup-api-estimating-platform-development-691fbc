package com.mytegroup.api.dto.waitlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record WaitlistEventDto(
    @NotBlank(message = "Event is required")
    @Size(max = 100, message = "Event must be at most 100 characters")
    String event,
    Map<String, Object> meta,
    @Size(max = 255, message = "Source must be at most 255 characters")
    String source,
    @Size(max = 500, message = "Path must be at most 500 characters")
    String path
) {
    public WaitlistEventDto {
        if (event != null) {
            event = event.trim();
        }
        if (source != null) {
            source = source.trim();
        }
        if (path != null) {
            path = path.trim();
        }
    }
}
