package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public class AIGeneratedQuestion {
  @JsonPropertyDescription("The question to ask the user")
  @JsonProperty(required = true)
  public String question;

  @JsonPropertyDescription("The only correct option")
  @JsonProperty(required = true)
  public String correctOption;

  @JsonPropertyDescription("Some wrong options.")
  @JsonProperty(required = true)
  public List<String> wrongOptions;
}
