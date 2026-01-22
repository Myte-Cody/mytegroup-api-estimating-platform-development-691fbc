package com.mytegroup.api.mapper.orgtaxonomy;

import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyDto;
import com.mytegroup.api.dto.orgtaxonomy.PutOrgTaxonomyValueDto;
import com.mytegroup.api.entity.organization.OrgTaxonomy;
import com.mytegroup.api.entity.organization.embeddable.OrgTaxonomyValue;
import com.mytegroup.api.entity.core.Organization;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrgTaxonomyMapper {
    public OrgTaxonomy toEntity(PutOrgTaxonomyDto dto, Organization organization) {
        OrgTaxonomy taxonomy = new OrgTaxonomy();
        taxonomy.setOrganization(organization);
        List<OrgTaxonomyValue> values = dto.getValues().stream()
            .map(this::toValueEntity)
            .collect(Collectors.toList());
        taxonomy.setValues(values);
        return taxonomy;
    }

    private OrgTaxonomyValue toValueEntity(PutOrgTaxonomyValueDto dto) {
        OrgTaxonomyValue value = new OrgTaxonomyValue();
        value.setKey(dto.key());
        value.setLabel(dto.label());
        value.setSortOrder(dto.sortOrder());
        value.setColor(dto.color());
        // metadata is Map<String, Object>, need to convert to JSON string
        if (dto.metadata() != null) {
            // Convert Map to JSON string - using simple toString for now
            // In production, use ObjectMapper or similar for proper JSON serialization
            value.setMetadata(dto.metadata().toString());
        }
        return value;
    }
}

