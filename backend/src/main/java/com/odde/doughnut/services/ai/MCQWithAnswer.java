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
  @JsonAlias({"f0__multipleChoicesQuestion", "multipleChoicesQuestion"})
  private MultipleChoicesQuestion question = new MultipleChoicesQuestion();

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  @JsonAlias({"f1__correctChoiceIndex", "correctChoiceIndex"})
  private int solutionChoiceIndex;

  @JsonPropertyDescription(
      "If true, the order of choices must be preserved and must not be randomized.")
  @JsonProperty(required = true)
  @JsonAlias({"f2__strictChoiceOrder", "strictChoiceOrder"})
  private boolean strictChoiceOrder;

  @JsonPropertyDescription(
      "Internal summary of the specific focus-note knowledge point tested. Not shown to the learner.")
  private String testedFocus;

  @JsonPropertyDescription(
      "Internal explanation of why the solution choice is uniquely correct and the other choices are incorrect. Note ambiguity if any. Not shown to the learner.")
  private String validationRationale;

  @JsonIgnore
  public boolean isValid() {
    if (question == null) return false;
    if (question.getQuestionStem() == null || question.getQuestionStem().isBlank()) return false;
    if (question.getResponseChoices() == null) return false;
    int choicesCount = question.getResponseChoices().size();
    if (choicesCount == 0) return false;
    return solutionChoiceIndex >= 0 && solutionChoiceIndex < choicesCount;
  }
}
