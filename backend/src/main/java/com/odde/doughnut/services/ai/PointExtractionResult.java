package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonClassDescription(
    """
    Extract a point from the parent note and generate content for a new child note.
    The point will be expanded into a complete note with title and details.
    The parent note's details will be updated with a brief summary replacing the extracted content.
    """)
@Data
public class PointExtractionResult {
  @NotNull
  @JsonPropertyDescription("The title for the new child note")
  @JsonProperty(required = true)
  public String newNoteTitle;

  @NotNull
  @JsonPropertyDescription("The details for the new child note in markdown format")
  @JsonProperty(required = true)
  public String newNoteDetails;

  @NotNull
  @JsonPropertyDescription(
      "The updated details for the parent note with the extracted content replaced by a brief summary")
  @JsonProperty(required = true)
  public String updatedParentDetails;
}
