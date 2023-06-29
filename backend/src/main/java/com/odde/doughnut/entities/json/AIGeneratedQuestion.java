package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public class AIGeneratedQuestion {
  public class AIQuestionOption {
    @JsonPropertyDescription("The option to ask the user")
    public String option;

    @JsonPropertyDescription("Whether the option is correct or not")
    @JsonProperty(required = true)
    public Boolean correct;
  }

  @JsonPropertyDescription("The question to ask the user")
  @JsonProperty(required = true)
  public String question;

  @JsonPropertyDescription("The options to ask the user to choose from")
  @JsonProperty(required = true)
  public List<AIQuestionOption> options;
}
