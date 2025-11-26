package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonClassDescription("Ask a single-answer multiple-choice question to the user")
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
    if (multipleChoicesQuestion.getF0__stem() == null
        || multipleChoicesQuestion.getF0__stem().isBlank()) return false;
    if (multipleChoicesQuestion.getF1__choices() == null) return false;
    int choicesCount = multipleChoicesQuestion.getF1__choices().size();
    if (choicesCount == 0) return false;
    return correctChoiceIndex >= 0 && correctChoiceIndex < choicesCount;
  }
}
