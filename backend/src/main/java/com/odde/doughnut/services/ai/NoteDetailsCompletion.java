package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class NoteDetailsCompletion {

  @JsonPropertyDescription(
      "The completion of the note details to be appended to the existing note details. Add necessary white space or new line at the beginning to connect to existing details. Note that the existing details may be empty or end with incomplete word. Leave empty if the existing details are already complete.")
  @JsonProperty(required = true)
  public String completion;
}
