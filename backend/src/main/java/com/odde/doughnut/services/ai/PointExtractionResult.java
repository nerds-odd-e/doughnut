package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonClassDescription(
    """
    Extract a point from the parent note and generate content for a new sibling note.
    The point will be expanded into a complete note with title and content.
    The parent note's content will be updated with extracted content removed.
    """)
@Data
public class PointExtractionResult {
  @NotNull
  @JsonPropertyDescription("The title for the new sibling note")
  @JsonProperty(required = true)
  public String newNoteTitle;

  @NotNull
  @JsonPropertyDescription("The content for the new sibling note in markdown format")
  @JsonProperty(required = true)
  public String newNoteContent;

  @NotNull
  @JsonPropertyDescription(
      "The updated content for the parent note with the extracted content replaced by a brief summary")
  @JsonProperty(required = true)
  public String updatedParentContent;
}
