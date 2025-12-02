package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonClassDescription(
    "Text completion for the details of the note of focus using unified diff format")
@NoArgsConstructor
@AllArgsConstructor
public class NoteDetailsCompletion {

  @JsonPropertyDescription(
      "A unified diff patch in text format that describes the changes to apply to the existing note details. The patch should follow the standard unified diff format with lines starting with ' ' (space) for context, '-' for deletions, and '+' for additions. The patch should be provided as a single string with newlines. For example:\n"
          + "--- a\n"
          + "+++ b\n"
          + "@@ -1,3 +1,3 @@\n"
          + " It is a\n"
          + "-vigorous\n"
          + "+beautiful\n"
          + " city.\n"
          + "\n"
          + "The patch will be applied to the current note details to produce the completed version.")
  @JsonProperty(required = true)
  public String patch;
}
