package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class TextFromAudio {
  @JsonPropertyDescription(
      "The text from the audio transcript (SRT) to complete the previous note details (if any), which will be appended to the existing note details. Add necessary white space or new line at the beginning to connect to existing details. The context should be in markdown format.")
  @JsonProperty(required = true)
  private String textFromAudio;
}
