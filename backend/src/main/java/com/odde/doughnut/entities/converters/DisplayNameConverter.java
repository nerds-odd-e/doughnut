package com.odde.doughnut.entities.converters;

import com.odde.doughnut.entities.DisplayName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DisplayNameConverter implements AttributeConverter<DisplayName, String> {

  @Override
  public String convertToDatabaseColumn(DisplayName attribute) {
    return attribute == null ? "" : attribute.value();
  }

  @Override
  public DisplayName convertToEntityAttribute(String dbData) {
    return new DisplayName(dbData);
  }
}
