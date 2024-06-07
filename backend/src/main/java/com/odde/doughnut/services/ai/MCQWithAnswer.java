package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonIgnoreProperties({"confidence"})
public class MCQWithAnswer extends MultipleChoicesQuestion {

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  public int correctChoiceIndex;

  @JsonIgnore
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    MultipleChoicesQuestion clone = new MultipleChoicesQuestion();
    clone.stem = stem;
    clone.choices = choices;
    return clone;
  }
}
