package com.mytegroup.api.dto.legal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLegalDocDto {
    
    @NotBlank(message = "Type is required")
    private String type;
    
    @NotBlank(message = "Version is required")
    @Size(min = 1, message = "Version must be at least 1 character")
    private String version;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Content is required")
    @Size(min = 10, message = "Content must be at least 10 characters")
    private String content;
    
    private LocalDateTime effectiveAt;
    
    public void setVersion(String version) {
        this.version = version != null ? version.trim() : null;
    }
    
    public void setContent(String content) {
        this.content = content != null ? content.trim() : null;
    }
}
