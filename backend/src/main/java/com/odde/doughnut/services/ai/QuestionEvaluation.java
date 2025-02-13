package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import java.util.Arrays;
import java.util.stream.Collectors;

public class QuestionEvaluation {
  @JsonPropertyDescription("Indices of the correct choices. 0-based.")
  @JsonProperty(required = true)
  public int[] correctChoices;

  @JsonPropertyDescription("Whether the question is feasible.")
  @JsonProperty(required = true)
  public boolean feasibleQuestion;

  @JsonPropertyDescription(
      "Explains why the question is not feasible and advises for improvement. Leave empty if the question is feasible.")
  @JsonProperty(required = true)
  public String improvementAdvices;

  private boolean indisputableAnswer(int correctChoiceIndex) {
    return correctChoices != null
        && correctChoices.length == 1
        && correctChoices[0] == correctChoiceIndex;
  }

  public QuestionContestResult getQuestionContestResult(Integer correctChoiceIndex) {
    if (feasibleQuestion && indisputableAnswer(correctChoiceIndex)) {
      QuestionContestResult result = new QuestionContestResult();
      result.advice = "This seems to be a legitimate question. Please answer it.";
      result.rejected = true;
      return result;
    }
    QuestionContestResult result = new QuestionContestResult();
    result.advice = improvementAdvices == null ? "" : improvementAdvices;
    if (!indisputableAnswer(correctChoiceIndex)) {
      String correctChoicesStr =
          correctChoices == null
              ? "none"
              : Arrays.stream(correctChoices)
                  .mapToObj(String::valueOf)
                  .collect(Collectors.joining(", "));
      result.advice +=
          "\n\nUnclear answer detected. The original question assume one correct choice index (0-based) of "
              + correctChoiceIndex
              + ". however, the re-evaluation of the question shows that "
              + correctChoicesStr
              + " are correct to the question.";
    }
    return result;
  }
}
