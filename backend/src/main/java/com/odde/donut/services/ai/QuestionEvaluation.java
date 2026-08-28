package com.odde.donut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.Mcq;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonClassDescription("answer and evaluate the question to check its quality")
@JsonIgnoreProperties(ignoreUnknown = true)
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

  public QuestionContestResult getQuestionContestResult(Mcq mcq) {
    int correctChoiceIndex = mcq.getCorrectAnswerIndex() == null ? -1 : mcq.getCorrectAnswerIndex();
    if (feasibleQuestion && indisputableAnswer(correctChoiceIndex)) {
      QuestionContestResult result = new QuestionContestResult();
      result.advice = "This seems to be a legitimate question. Please answer it.";
      result.rejected = true;
      return result;
    }
    QuestionContestResult result = new QuestionContestResult();
    result.advice = "";
    if (!indisputableAnswer(correctChoiceIndex)) {
      var choices = mcq.getResponseChoices();
      if (choices == null) {
        result.advice = "The question has no choices defined.";
        return result;
      }
      result.advice =
          "Unclear answer detected. The original question assume one correct choice of "
              + quotedOriginalChoice(correctChoiceIndex, choices)
              + ". however, the re-evaluation of the question shows that "
              + quotedCorrectChoices(correctChoices, choices)
              + " are correct to the question.\n"
              + "Please make sure the correct answer is correct and unique.\n\n";
    }
    result.advice += improvementAdvices == null ? "" : improvementAdvices;
    return result;
  }

  private static String quotedOriginalChoice(int correctChoiceIndex, List<String> choices) {
    if (correctChoiceIndex >= 0 && correctChoiceIndex < choices.size()) {
      return "\"" + choices.get(correctChoiceIndex) + "\"";
    }
    return "\"unknown\"";
  }

  private static String quotedCorrectChoices(int[] correctChoices, List<String> choices) {
    if (correctChoices == null) {
      return "none";
    }
    String quoted =
        Arrays.stream(correctChoices)
            .filter(i -> i >= 0 && i < choices.size())
            .mapToObj(i -> "\"" + choices.get(i) + "\"")
            .collect(Collectors.joining(", "));
    return quoted.isEmpty() ? "none" : quoted;
  }
}
