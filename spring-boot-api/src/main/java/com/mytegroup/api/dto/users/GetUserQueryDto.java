package com.mytegroup.api.dto.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUserQueryDto {
    
    private Boolean includeArchived;
}
