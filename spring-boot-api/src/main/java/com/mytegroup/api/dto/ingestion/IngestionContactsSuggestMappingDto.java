package com.mytegroup.api.dto.ingestion;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionContactsSuggestMappingDto {
    
    private String profile;
    
    @NotNull(message = "Headers are required")
    @NotEmpty(message = "At least one header is required")
    @Size(max = 200, message = "Headers must not exceed 200 items")
    private List<String> headers;
    
    private List<Map<String, Object>> sampleRows;
    
    private Boolean allowAiProcessing;
    
    public void setProfile(String profile) {
        this.profile = profile != null ? profile.trim() : null;
    }
}
