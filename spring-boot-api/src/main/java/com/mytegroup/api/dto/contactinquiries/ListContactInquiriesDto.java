package com.mytegroup.api.dto.contactinquiries;

import com.mytegroup.api.entity.enums.communication.ContactInquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListContactInquiriesDto {
    
    private ContactInquiryStatus status;
    
    private String emailContains;
    
    private Integer page;
    
    private Integer limit;
}
