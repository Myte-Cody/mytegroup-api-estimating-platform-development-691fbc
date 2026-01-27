package com.mytegroup.api.entity.enums.organization;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DataResidencyConverter implements AttributeConverter<DataResidency, String> {

    @Override
    public String convertToDatabaseColumn(DataResidency attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public DataResidency convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DataResidency.fromValue(dbData);
    }
}
