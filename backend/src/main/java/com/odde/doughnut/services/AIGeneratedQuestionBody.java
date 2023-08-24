package com.odde.doughnut.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class AIGeneratedQuestionBody {
  @JsonPropertyDescription(
      "The stem of the multiple-choice question. Provide background or disclosure necessary to clarify the question when needed.")
  @JsonProperty(required = false)
  public String stem;

  @JsonPropertyDescription("All choices. Only one should be correct.")
  @JsonProperty(required = true)
  public List<String> choices;

  @JsonPropertyDescription("All choices' reason.")
  @JsonProperty(required = true)
  public List<String> reasons;

  public String toJsonString() {
    return new ObjectMapper().valueToTree(this).toString();
  }
}
