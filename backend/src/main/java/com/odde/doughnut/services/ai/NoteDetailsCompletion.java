package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class NoteDetailsCompletion {

  @JsonPropertyDescription(
      "The completion of the note details to be appended to the existing note details. Add necessary white space or new line at the beginning to connect to existing details. The context should be in markdown format.")
  @JsonProperty(required = true)
  public String completion;

  @JsonPropertyDescription(
      "Number of characters to delete from the end of existing details before appending the completion. Use this to remove trailing spaces, incomplete words, or punctuation. Default is 0.")
  @JsonProperty(defaultValue = "0")
  public Integer deleteFromEnd = 0;
}
