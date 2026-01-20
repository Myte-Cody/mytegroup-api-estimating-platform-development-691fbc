package com.mytegroup.api.mapper.estimates;

import com.mytegroup.api.dto.estimates.CreateEstimateDto;
import com.mytegroup.api.dto.estimates.EstimateLineItemDto;
import com.mytegroup.api.dto.estimates.UpdateEstimateDto;
import com.mytegroup.api.entity.core.Organization;
import com.mytegroup.api.entity.core.User;
import com.mytegroup.api.entity.enums.projects.EstimateStatus;
import com.mytegroup.api.entity.projects.Estimate;
import com.mytegroup.api.entity.projects.Project;
import com.mytegroup.api.entity.projects.embeddable.EstimateLineItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EstimateMapper {

    /**
     * Maps CreateEstimateDto to Estimate entity.
     */
    public Estimate toEntity(CreateEstimateDto dto, Project project, Organization organization, User createdByUser) {
        Estimate estimate = new Estimate();
        estimate.setProject(project);
        estimate.setOrganization(organization);
        estimate.setCreatedByUser(createdByUser);
        estimate.setName(dto.name());
        estimate.setDescription(dto.description());
        estimate.setNotes(dto.notes());
        estimate.setStatus(EstimateStatus.DRAFT);
        
        // Map line items
        if (dto.lineItems() != null && !dto.lineItems().isEmpty()) {
            List<EstimateLineItem> lineItems = dto.lineItems().stream()
                .map(this::toLineItemEntity)
                .collect(Collectors.toList());
            estimate.setLineItems(lineItems);
        } else {
            estimate.setLineItems(new ArrayList<>());
        }
        
        return estimate;
    }

    /**
     * Updates existing Estimate entity with UpdateEstimateDto values.
     */
    public void updateEntity(Estimate estimate, UpdateEstimateDto dto) {
        if (dto.name() != null) {
            estimate.setName(dto.name());
        }
        if (dto.description() != null) {
            estimate.setDescription(dto.description());
        }
        if (dto.notes() != null) {
            estimate.setNotes(dto.notes());
        }
        if (dto.status() != null) {
            estimate.setStatus(dto.status());
        }
        
        // Update line items if provided
        if (dto.lineItems() != null) {
            List<EstimateLineItem> lineItems = dto.lineItems().stream()
                .map(this::toLineItemEntity)
                .collect(Collectors.toList());
            estimate.setLineItems(lineItems);
        }
    }

    private EstimateLineItem toLineItemEntity(EstimateLineItemDto dto) {
        EstimateLineItem item = new EstimateLineItem();
        item.setCode(dto.code());
        item.setDescription(dto.description());
        item.setQuantity(dto.quantity());
        item.setUnit(dto.unit());
        item.setUnitCost(dto.unitCost());
        item.setTotal(dto.total());
        return item;
    }
}

