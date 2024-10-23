package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class TextFromAudio {
  @JsonPropertyDescription("text from audio")
  @JsonProperty(required = true)
  private String textFromAudio;
}
