package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
  @JsonProperty(required = true)
  private boolean strictChoiceOrder;

  @JsonIgnore
  public boolean isValid() {
    if (multipleChoicesQuestion == null) return false;
    if (multipleChoicesQuestion.getStem() == null || multipleChoicesQuestion.getStem().isBlank())
      return false;
    if (multipleChoicesQuestion.getChoices() == null) return false;
    int choicesCount = multipleChoicesQuestion.getChoices().size();
    if (choicesCount == 0) return false;
    return correctChoiceIndex >= 0 && correctChoiceIndex < choicesCount;
  }
}
