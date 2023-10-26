package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
      return Optional.empty();
    }
  }

  public boolean makeSense(int correctChoiceIndex) {
    return feasibleQuestion
        && correctChoices != null
        && correctChoices.length == 1
        && correctChoices[0] == correctChoiceIndex;
  }
}
