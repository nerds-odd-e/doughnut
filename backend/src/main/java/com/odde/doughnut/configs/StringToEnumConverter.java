package com.odde.doughnut.configs;

import com.odde.doughnut.entities.RelationType;
import org.springframework.core.convert.converter.Converter;

public class StringToEnumConverter implements Converter<String, RelationType> {

  @Override
  public RelationType convert(String source) {
    return RelationType.fromLabel(source);
  }
}
