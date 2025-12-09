package com.odde.doughnut.entities.converters;

import com.odde.doughnut.entities.NoteType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NoteTypeConverter implements AttributeConverter<NoteType, String> {

  @Override
  public String convertToDatabaseColumn(NoteType noteType) {
    return noteType == null ? null : noteType.label;
  }

  @Override
  public NoteType convertToEntityAttribute(String dbData) {
    return dbData == null ? null : NoteType.fromLabel(dbData);
  }
}
