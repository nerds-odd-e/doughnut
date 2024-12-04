package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MCQWithAnswer {

  @JsonProperty(required = true)
  private MultipleChoicesQuestion multipleChoicesQuestion = new MultipleChoicesQuestion();

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  private int correctChoiceIndex;

  @JsonPropertyDescription(
      "If true, the order of choices should not be randomized due to interdependent statements like 'None of the above'")
  @JsonProperty(defaultValue = "false")
  private boolean strictChoiceOrder = false;
}
