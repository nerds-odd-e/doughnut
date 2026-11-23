package com.odde.donut.entities.converters;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.DailyProbeTrialDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class DailyProbeTrialsConverter
    implements AttributeConverter<List<DailyProbeTrialDTO>, String> {

  private final JsonMapper jsonMapper = ObjectMapperConfig.handWrittenJsonMapper();

  @Override
  public String convertToDatabaseColumn(List<DailyProbeTrialDTO> trials) {
    return jsonMapper.writeValueAsString(trials);
  }

  @Override
  public List<DailyProbeTrialDTO> convertToEntityAttribute(String json) {
    return jsonMapper.readValue(json, new TypeReference<>() {});
  }
}
