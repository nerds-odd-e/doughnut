package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TopicTitleGeneration {
  @JsonProperty("title")
  public String title;
}
