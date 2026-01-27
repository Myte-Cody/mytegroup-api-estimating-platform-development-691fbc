package com.mytegroup.api.entity.enums.people;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PersonTypeConverter implements AttributeConverter<PersonType, String> {

    @Override
    public String convertToDatabaseColumn(PersonType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PersonType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PersonType.fromValue(dbData);
    }
}
