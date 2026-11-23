package com.odde.donut.entities.converters;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.entities.MultipleChoicesQuestion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class MCQToJsonConverter implements AttributeConverter<MultipleChoicesQuestion, String> {

  private final JsonMapper jsonMapper =
      ObjectMapperConfig.handWrittenJsonMapper()
          .rebuild()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  @Override
  public String convertToDatabaseColumn(MultipleChoicesQuestion attribute) {
    try {
      return jsonMapper.writeValueAsString(attribute);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Error converting list to JSON", e);
    }
  }

  @Override
  public MultipleChoicesQuestion convertToEntityAttribute(String dbData) {
    try {
      return jsonMapper.readValue(dbData, MultipleChoicesQuestion.class);
    } catch (JacksonException e) {
      // for backward compatibility
      return new MultipleChoicesQuestion();
    }
  }
}
