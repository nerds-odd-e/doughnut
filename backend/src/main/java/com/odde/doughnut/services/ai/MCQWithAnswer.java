package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class MCQWithAnswer {

  @JsonPropertyDescription("Question stem and choices.")
  @JsonProperty(required = true)
  private MultipleChoicesQuestion multipleChoicesQuestion = new MultipleChoicesQuestion();

  @JsonPropertyDescription("Index of the correct choice. 0-based.")
  @JsonProperty(required = true)
  private int correctChoiceIndex;

  @JsonPropertyDescription("is_approved")
  @JsonProperty
  private boolean approved;

  @JsonPropertyDescription("id")
  @JsonProperty
  private Integer id;
}
