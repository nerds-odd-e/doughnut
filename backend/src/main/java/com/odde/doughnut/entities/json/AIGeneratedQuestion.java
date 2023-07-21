package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.models.quizFacotries.QuizQuestionNotPossibleException;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.Nullable;

public class AIGeneratedQuestion {

  @JsonPropertyDescription("The stem of the multiple-choice question")
  @JsonProperty(required = false)
  public String stem;

  @JsonPropertyDescription("The only correct choice")
  @JsonProperty(required = true)
  public String correctChoice;

  @JsonPropertyDescription("The incorrect choices.")
  @JsonProperty(required = true)
  public List<String> incorrectChoices;

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
}
