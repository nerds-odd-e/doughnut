package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;

public class AIGeneratedQuestion {

  @JsonPropertyDescription("The stem of the multiple-choice question")
  @JsonProperty(required = false)
  public String stem;

  @JsonPropertyDescription("All choices. Only one should be correct.")
  @JsonProperty(required = true)
  public List<String> choices;

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  public int correctChoiceIndex;

  @JsonPropertyDescription(
      "Background information or disclosure necessary to clarify the question. Use only if the stem would be unclear or ambiguous without this information. Will be put before stem.")
  @JsonProperty(required = false)
  @Nullable
  public String background;

  public AIGeneratedQuestion validateQuestion() throws QuizQuestionNotPossibleException {
    if (stem != null && !Strings.isBlank(stem)) {
      return this;
    }
    throw new QuizQuestionNotPossibleException();
  }

  public String toJsonString() {
    return new ObjectMapper().valueToTree(this).toString();
  }
}
