package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonClassDescription(
    "Text completion for the details of the note of focus. Provide a unified diff (text format) showing the changes to apply to the existing note details. The diff should use standard unified diff format with lines prefixed by '-' for deletions, '+' for additions, and ' ' (space) for unchanged context lines.")
@NoArgsConstructor
@AllArgsConstructor
public class NoteDetailsCompletion {

  @JsonPropertyDescription(
      "A unified diff (text format) representing the changes to apply to the existing note details. The diff should be in the standard unified diff format with lines starting with '-' for deletions, '+' for additions, and ' ' for unchanged context lines. The diff should show how to transform the current note details into the completed version. The patch should be provided as plain text in unified diff format.")
  @JsonProperty(required = true)
  public String patch;
}
