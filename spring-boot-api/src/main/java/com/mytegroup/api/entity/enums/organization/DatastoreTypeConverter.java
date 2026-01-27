package com.mytegroup.api.entity.enums.organization;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DatastoreTypeConverter implements AttributeConverter<DatastoreType, String> {

    @Override
    public String convertToDatabaseColumn(DatastoreType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public DatastoreType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DatastoreType.fromValue(dbData);
    }
}
