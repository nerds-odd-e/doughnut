package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonClassDescription(
    "Rewrite the given points into coherent note content. Output the complete new content in markdown format.")
@NoArgsConstructor
@AllArgsConstructor
public class RegeneratedNoteContent {

  @JsonPropertyDescription("The new note content in markdown, generated from the given points.")
  @JsonProperty(required = true)
  public String content;
}
