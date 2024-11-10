package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicTitleGeneration {
  @JsonProperty("topic title for the note")
  @NotNull
  public String title;
}
