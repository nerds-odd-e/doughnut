package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class AIGeneratedQuestion {

  @JsonPropertyDescription(
      "The stem of the multiple-choice question. Provide background or disclosure necessary to clarify the question when needed.")
  @JsonProperty(required = false)
  public String stem;

  @JsonPropertyDescription("All choices. Only one should be correct.")
  @JsonProperty(required = true)
  public List<String> choices;

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  public int correctChoiceIndex;

  public static AIGeneratedQuestion getValidQuestion(JsonNode question)
      throws QuizQuestionNotPossibleException {
    try {
      AIGeneratedQuestion aiGeneratedQuestion =
          new ObjectMapper().treeToValue(question, AIGeneratedQuestion.class);
      if (aiGeneratedQuestion.stem != null && !Strings.isBlank(aiGeneratedQuestion.stem)) {
        return aiGeneratedQuestion;
      }
    } catch (JsonProcessingException e) {
    }
    throw new QuizQuestionNotPossibleException();
  }

  public String toJsonString() {
    return new ObjectMapper().valueToTree(this).toString();
  }
}
