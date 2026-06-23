package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.odde.doughnut.validators.DisplayNamePathSeparators;
import java.io.IOException;

public class DisplayNameTrimmingDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return DisplayNamePathSeparators.trimSurroundingWhitespace(p.getValueAsString());
  }
}
