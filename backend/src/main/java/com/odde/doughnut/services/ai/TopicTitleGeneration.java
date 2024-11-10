package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicTitleGeneration {
  @NotNull
  @JsonPropertyDescription("The topic title for the note.")
  public String topic;
}
