package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonClassDescription("Text completion for the details of the note of focus")
@NoArgsConstructor
@AllArgsConstructor
public class NoteDetailsCompletion {

  @JsonPropertyDescription(
      "A non-negative integer (zero or positive) representing the number of characters to delete from the end of the existing note details before appending the completion. Use this to remove trailing spaces, new lines, empty paragraphs, incomplete words, or punctuation when needed. If set to 0, no deletion occurs. If 'deleteFromEnd' exceeds the length of the existing details, all existing content will be deleted. Characters are counted as Unicode code points.")
  @JsonProperty(required = true)
  public Integer deleteFromEnd;

  @JsonPropertyDescription(
      "The completion text to be appended to the existing note details after the specified deletion. Begin the completion with necessary whitespace or new lines to properly connect it to the existing details. If 'deleteFromEnd' removes part of a word, start the completion with the complete word to ensure continuity. The 'completion' should be provided in Markdown format. For example, if the existing details end with 'runni' and 'deleteFromEnd' is 5, the 'completion' should start with 'running'.")
  @JsonProperty(required = true)
  public String completion;
}
