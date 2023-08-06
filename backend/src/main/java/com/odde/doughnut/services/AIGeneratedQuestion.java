package com.odde.doughnut.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import org.apache.logging.log4j.util.Strings;

public class AIGeneratedQuestion extends AIGeneratedQuestionBody {

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  public int correctChoiceIndex;

  @JsonPropertyDescription("Confidence of the correctness of the question. 0 to 10.")
  @JsonProperty(required = true)
  public int confidence;

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
}
