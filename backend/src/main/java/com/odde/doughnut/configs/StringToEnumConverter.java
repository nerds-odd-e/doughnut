package com.odde.doughnut.configs;

import com.odde.doughnut.entities.Link.LinkType;
import org.springframework.core.convert.converter.Converter;

public class StringToEnumConverter implements Converter<String, LinkType> {

  @Override
  public LinkType convert(String source) {
    return LinkType.fromLabel(source);
  }
}
