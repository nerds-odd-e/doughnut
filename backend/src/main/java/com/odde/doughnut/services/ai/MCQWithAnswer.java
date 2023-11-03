package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import org.apache.logging.log4j.util.Strings;

@JsonIgnoreProperties({"confidence"})
public class MCQWithAnswer extends MultipleChoicesQuestion {

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  public int correctChoiceIndex;

  public static MCQWithAnswer getValidQuestion(JsonNode question)
      throws QuizQuestionNotPossibleException {
    try {
      MCQWithAnswer MCQWithAnswer = new ObjectMapper().treeToValue(question, MCQWithAnswer.class);
      if (MCQWithAnswer.stem != null && !Strings.isBlank(MCQWithAnswer.stem)) {
        return MCQWithAnswer;
      }
    } catch (JsonProcessingException e) {
    }
    throw new QuizQuestionNotPossibleException();
  }
}
