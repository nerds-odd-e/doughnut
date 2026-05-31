package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonClassDescription(
    """
    Extract a refinement suggestion from the note and generate content for a new note.
    The suggestion will be expanded into a complete note with title and content.
    The original note's content will be updated with extracted content removed.
    """)
@Data
public class NoteExtractionResult {
  @NotNull
  @JsonPropertyDescription("The title for the new note")
  @JsonProperty(required = true)
  public String newNoteTitle;

  @NotNull
  @JsonPropertyDescription("The content for the new note in markdown format")
  @JsonProperty(required = true)
  public String newNoteContent;

  @NotNull
  @JsonPropertyDescription(
      "The updated content for the original note with the extracted content removed or summarized")
  @JsonProperty(required = true)
  public String updatedParentContent;
}
