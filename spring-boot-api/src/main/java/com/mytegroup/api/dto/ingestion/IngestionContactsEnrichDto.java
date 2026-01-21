package com.mytegroup.api.dto.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngestionContactsEnrichDto {
    
    @NotBlank(message = "Profile is required")
    private String profile;
    
    @NotNull(message = "Contact is required")
    private Map<String, Object> contact;
    
    private Boolean allowAiProcessing;
    
    public void setProfile(String profile) {
        this.profile = profile != null ? profile.trim() : null;
    }
}
