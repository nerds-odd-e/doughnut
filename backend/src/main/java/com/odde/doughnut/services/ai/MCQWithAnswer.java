package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties({"confidence"})
@EqualsAndHashCode
public class MCQWithAnswer {

  @JsonPropertyDescription("Question stem and choices.")
  @JsonProperty(required = true)
  public MultipleChoicesQuestion multipleChoicesQuestion = new MultipleChoicesQuestion();

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  public int correctChoiceIndex;
}
