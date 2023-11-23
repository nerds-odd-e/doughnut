package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ClarifyingQuestion {

  @JsonPropertyDescription("content of the question as plain string.")
  @JsonProperty(required = true)
  public String question;
}
