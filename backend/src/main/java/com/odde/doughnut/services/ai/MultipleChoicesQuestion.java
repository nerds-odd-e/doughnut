package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import lombok.Data;

@Data
public class MultipleChoicesQuestion {
  @JsonPropertyDescription(
      "The stem of the multiple-choice question. Provide background or disclosure necessary to clarify the question when needed.")
  @JsonProperty(required = true)
  private String stem;

  @JsonPropertyDescription("All choices. Only one should be correct.")
  @JsonProperty(required = true)
  private List<String> choices;
}
