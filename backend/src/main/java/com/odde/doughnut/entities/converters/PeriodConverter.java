package com.odde.doughnut.entities.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Period;

@Converter
public class PeriodConverter implements AttributeConverter<Period, String> {

  @Override
  public String convertToDatabaseColumn(Period period) {
    return period == null ? null : period.toString();
  }

  @Override
  public Period convertToEntityAttribute(String dbData) {
    return dbData == null ? null : Period.parse(dbData);
  }
}
