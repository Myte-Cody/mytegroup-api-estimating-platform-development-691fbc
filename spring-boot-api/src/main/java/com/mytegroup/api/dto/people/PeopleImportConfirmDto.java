package com.mytegroup.api.dto.people;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeopleImportConfirmDto {
    
    private String previewId;
    
    @NotNull(message = "Rows are required")
    @NotEmpty(message = "At least one row is required")
    @Size(max = 1000, message = "Rows must not exceed 1000 items")
    @Valid
    private List<PeopleImportConfirmRowDto> confirmedRows;
}
