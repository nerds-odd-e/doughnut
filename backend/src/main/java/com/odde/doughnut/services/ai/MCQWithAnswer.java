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
  private MultipleChoicesQuestion question = new MultipleChoicesQuestion();

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  private int solutionChoiceIndex;

  @JsonPropertyDescription(
      "Whether choices can be safely reordered before display. False if any choice depends on position or refers to another choice.")
  @JsonProperty(required = true)
  private boolean choicesMayBeShuffled = true;

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
