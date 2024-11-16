package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class TextFromAudio {
  @JsonPropertyDescription(
      "The combined text from the audio transcript (SRT) to complete the previous text (previousTrailingNoteDetails. Add necessary white space or new line at the beginning to connect to the previous text. The context should be in markdown format.")
  @JsonProperty(required = true)
  private String completionMarkdownFromAudio;
}
