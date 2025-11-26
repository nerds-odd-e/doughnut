package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
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
  @JsonAlias("stem")
  private String f0__stem;

  @JsonPropertyDescription("List of choices. Markdown allowed.")
  @JsonProperty(required = true)
  @JsonAlias("choices")
  private List<String> f1__choices;
}
