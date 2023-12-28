package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;

@JsonIgnoreProperties({"confidence"})
public class MCQWithAnswer extends MultipleChoicesQuestion {

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  public int correctChoiceIndex;

  public static Optional<MCQWithAnswer> getValidQuestion(JsonNode question) {
    try {
      MCQWithAnswer mcqWithAnswer = new ObjectMapper().treeToValue(question, MCQWithAnswer.class);
      if (mcqWithAnswer.validQuestion()) {
        return Optional.of(mcqWithAnswer);
      }
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }

  @JsonIgnore
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    MultipleChoicesQuestion clone = new MultipleChoicesQuestion();
    clone.stem = stem;
    clone.choices = choices;
    return clone;
  }

  private boolean validQuestion() {
    return this.stem != null && !Strings.isBlank(this.stem);
  }
}
