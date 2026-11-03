package com.odde.donut.entities.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.DailyProbeTrialDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import lombok.SneakyThrows;

@Converter
public class DailyProbeTrialsConverter
    implements AttributeConverter<List<DailyProbeTrialDTO>, String> {

  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  @Override
  @SneakyThrows
  public String convertToDatabaseColumn(List<DailyProbeTrialDTO> trials) {
    return objectMapper.writeValueAsString(trials);
  }

  @Override
  @SneakyThrows
  public List<DailyProbeTrialDTO> convertToEntityAttribute(String json) {
    return objectMapper.readValue(json, new TypeReference<>() {});
  }
}
