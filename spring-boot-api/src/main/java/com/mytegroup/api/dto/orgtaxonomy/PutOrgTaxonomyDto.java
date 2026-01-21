package com.mytegroup.api.dto.orgtaxonomy;

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
public class PutOrgTaxonomyDto {
    
    @NotNull(message = "Values are required")
    @NotEmpty(message = "At least one value is required")
    @Size(max = 1000, message = "Values must not exceed 1000 items")
    @Valid
    private List<PutOrgTaxonomyValueDto> values;
}
