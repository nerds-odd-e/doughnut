package com.odde.doughnut.entities.converters;

import com.odde.doughnut.entities.RelationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RelationTypeConverter implements AttributeConverter<RelationType, String> {

  @Override
  public String convertToDatabaseColumn(RelationType relationType) {
    return relationType == null ? null : relationType.label;
  }

  @Override
  public RelationType convertToEntityAttribute(String dbData) {
    return dbData == null ? null : RelationType.fromLabel(dbData);
  }
}
