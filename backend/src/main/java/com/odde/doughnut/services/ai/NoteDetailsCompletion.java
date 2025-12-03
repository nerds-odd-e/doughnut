package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonClassDescription(
    """
    Replace the entire `details` field of the current focus note with the new content.
    The details should be in markdown format.
    
    IMPORTANT: Keep all parts of the existing details that the user did not ask to change.
    Copy those unchanged parts exactly as they appear in the original details, without any
    modifications. Only modify or add the parts that the user specifically requested to change.
    """)
@NoArgsConstructor
@AllArgsConstructor
public class NoteDetailsCompletion {

  @JsonPropertyDescription(
      "The complete new details text that will replace the current note's details.")
  @JsonProperty(required = true)
  public String details;
}
