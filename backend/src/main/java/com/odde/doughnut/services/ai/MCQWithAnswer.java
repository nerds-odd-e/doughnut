package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
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
  @JsonAlias("multipleChoicesQuestion")
  private MultipleChoicesQuestion f0__multipleChoicesQuestion = new MultipleChoicesQuestion();

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  @JsonAlias("correctChoiceIndex")
  private int f1__correctChoiceIndex;

  @JsonPropertyDescription(
      "If true, the order of choices must be preserved and must not be randomized.")
  @JsonProperty(required = true)
  @JsonAlias("strictChoiceOrder")
  private boolean f2__strictChoiceOrder;

  @JsonIgnore
  public boolean isValid() {
    if (f0__multipleChoicesQuestion == null) return false;
    if (f0__multipleChoicesQuestion.getF0__stem() == null
        || f0__multipleChoicesQuestion.getF0__stem().isBlank()) return false;
    if (f0__multipleChoicesQuestion.getF1__choices() == null) return false;
    int choicesCount = f0__multipleChoicesQuestion.getF1__choices().size();
    if (choicesCount == 0) return false;
    return f1__correctChoiceIndex >= 0 && f1__correctChoiceIndex < choicesCount;
  }
}
