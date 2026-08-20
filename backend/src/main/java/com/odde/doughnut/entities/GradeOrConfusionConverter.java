package com.odde.doughnut.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persists {@link Grade} names and {@code CONFUSION} in {@code product_outcome}. {@code null} Grade
 * means CONFUSION (not a grade).
 */
@Converter(autoApply = false)
public class GradeOrConfusionConverter implements AttributeConverter<Grade, String> {
  static final String CONFUSION = "CONFUSION";

  @Override
  public String convertToDatabaseColumn(Grade grade) {
    return grade == null ? CONFUSION : grade.name();
  }

  @Override
  public Grade convertToEntityAttribute(String dbData) {
    if (dbData == null || CONFUSION.equals(dbData)) {
      return null;
    }
    return Grade.valueOf(dbData);
  }
}
