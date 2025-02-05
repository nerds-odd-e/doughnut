package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.controllers.dto.QuestionContestResult;

public class QuestionEvaluation {
  @JsonPropertyDescription("Indices of the correct choices. 0-based.")
  @JsonProperty(required = true)
  public int correctChoices[];

  @JsonPropertyDescription("Whether the question is feasible.")
  @JsonProperty(required = true)
  public boolean feasibleQuestion;

  @JsonPropertyDescription(
      "Explains why the question is not feasible. Leave empty if the question is feasible.")
  @JsonProperty(required = true)
  public String comment;

  private boolean indisputableAnswer(int correctChoiceIndex) {
    return correctChoices != null
        && correctChoices.length == 1
        && correctChoices[0] == correctChoiceIndex;
  }

  public QuestionContestResult getQuestionContestResult(Integer correctAnswerIndex) {
    if (feasibleQuestion && indisputableAnswer(correctAnswerIndex)) {
      QuestionContestResult result = new QuestionContestResult();
      result.reason = "This seems to be a legitimate question. Please answer it.";
      result.rejected = true;
      return result;
    }
    QuestionContestResult result = new QuestionContestResult();
    result.reason = comment;
    if (!indisputableAnswer(correctAnswerIndex)) {
      result.reason += " Uncleared answer detected.";
    }
    return result;
  }
}
