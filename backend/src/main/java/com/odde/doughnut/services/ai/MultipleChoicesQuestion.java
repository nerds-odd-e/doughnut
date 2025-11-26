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
      "The question stem — the full, self-contained text of the prompt. Must not reference external context.")
  @JsonProperty(required = true)
  @JsonAlias("stem")
  private String f0__stem;

  @JsonPropertyDescription("List of choices. Markdown allowed.")
  @JsonProperty(required = true)
  @JsonAlias("choices")
  private List<String> f1__choices;
}
