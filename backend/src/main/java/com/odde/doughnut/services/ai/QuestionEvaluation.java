package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.ReviewQuestionContestResult;
import java.util.Optional;

public class QuestionEvaluation {
  @JsonPropertyDescription("Indices of the correct choices. 0-based.")
  @JsonProperty(required = true)
  public int correctChoices[];

  @JsonPropertyDescription("Whether the question is feasible.")
  @JsonProperty(required = true)
  public boolean feasibleQuestion;

  @JsonPropertyDescription("Explains why the question is not feasible.")
  public String comment;

  public static Optional<QuestionEvaluation> getQuestionEvaluation(JsonNode jsonNode) {
    try {
      return Optional.of(new ObjectMapper().treeToValue(jsonNode, QuestionEvaluation.class));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean indisputableAnswer(int correctChoiceIndex) {
    return correctChoices != null
        && correctChoices.length == 1
        && correctChoices[0] == correctChoiceIndex;
  }

  public ReviewQuestionContestResult getReviewQuestionContestResult(Integer correctAnswerIndex) {
    if (feasibleQuestion && indisputableAnswer(correctAnswerIndex)) {
      ReviewQuestionContestResult result = new ReviewQuestionContestResult();
      result.reason = "This seems to be a legitimate question. Please answer it.";
      result.rejected = true;
      return result;
    }
    ReviewQuestionContestResult result = new ReviewQuestionContestResult();
    result.reason = comment;
    if (!indisputableAnswer(correctAnswerIndex)) {
      result.reason += " Uncleared answer detected.";
    }
    return result;
  }
}
