package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonClassDescription(
    "Rewrite the given points into coherent note details. Output the complete new details in markdown format.")
@NoArgsConstructor
@AllArgsConstructor
public class RegeneratedNoteDetails {

  @JsonPropertyDescription("The new note details in markdown, generated from the given points.")
  @JsonProperty(required = true)
  public String details;
}
