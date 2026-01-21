package com.mytegroup.api.dto.legal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptLegalDocDto {
    
    @NotNull(message = "Document ID is required")
    private Long docId;
    
    private String orgId;
    
    private String version;
    
    public void setVersion(String version) {
        this.version = version != null ? version.trim() : null;
    }
}
