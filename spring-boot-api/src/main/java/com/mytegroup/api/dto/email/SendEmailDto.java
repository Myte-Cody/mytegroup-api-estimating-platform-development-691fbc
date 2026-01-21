package com.mytegroup.api.dto.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String to;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String text;
    
    private String body;
    
    private String html;
    
    private String templateName;
    
    private String orgId;
    
    private Map<String, Object> variables;
    
    private String mode;
    
    private List<@Email String> bcc;
    
    public void setTo(String to) {
        this.to = to != null ? to.toLowerCase().trim() : null;
    }
    
    public void setSubject(String subject) {
        this.subject = subject != null ? subject.trim() : null;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName != null ? templateName.trim() : null;
    }
    
    public void setOrgId(String orgId) {
        this.orgId = orgId != null ? orgId.trim() : null;
    }
}
