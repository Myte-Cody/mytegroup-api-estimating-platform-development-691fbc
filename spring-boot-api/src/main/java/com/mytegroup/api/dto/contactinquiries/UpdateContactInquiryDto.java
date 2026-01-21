package com.mytegroup.api.dto.contactinquiries;

import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContactInquiryDto {
    
    private ContactInquiryStatus status;
    
    private String note;
    
    private String responderId;
    
    public void setResponderId(String responderId) {
        this.responderId = responderId != null ? responderId.trim() : null;
    }
}
