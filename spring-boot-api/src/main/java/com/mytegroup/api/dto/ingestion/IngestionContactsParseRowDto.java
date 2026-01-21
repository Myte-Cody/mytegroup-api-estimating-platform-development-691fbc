package com.mytegroup.api.dto.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionContactsParseRowDto {
    
    @NotBlank(message = "Profile is required")
    private String profile;
    
    @NotNull(message = "Row is required")
    private Map<String, Object> row;
    
    private Map<String, String> mapping;
    
    private Boolean allowAiProcessing;
    
    public void setProfile(String profile) {
        this.profile = profile != null ? profile.trim() : null;
    }
}
