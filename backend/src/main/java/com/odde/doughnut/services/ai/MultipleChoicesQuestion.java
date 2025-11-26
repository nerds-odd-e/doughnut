package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultipleChoicesQuestion {
  @JsonPropertyDescription(
      "The stem of the multiple-choice question. Accepts **Markdown** notation. Provide background or disclosure necessary to clarify the question when needed.")
  @JsonProperty(required = true)
  private String stem;

  @JsonPropertyDescription("List of choices. Markdown allowed.")
  @JsonProperty(required = true)
  private List<String> choices;
}
