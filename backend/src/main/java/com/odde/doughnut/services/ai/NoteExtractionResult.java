package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@JsonClassDescription(
    """
    Extract one or more selected layout points from the note into one new note.
    The selected points will be expanded into a complete note with title and content.
    The original note's content will be updated with the extracted content removed.
    """)
@Data
public class NoteExtractionResult {
  @NotNull
  @JsonPropertyDescription("The title for the new note")
  @JsonProperty(required = true)
  public String newNoteTitle;

  @NotNull
  @JsonPropertyDescription(
      "Markdown body for the new note. Do not repeat newNoteTitle as a leading markdown heading;"
          + " the title is stored separately and shown by the UI.")
  @JsonProperty(required = true)
  public String newNoteContent;

  @NotNull
  @JsonPropertyDescription(
      "The updated content for the original note with the extracted content removed or summarized")
  @JsonProperty(required = true)
  public String updatedOriginalNoteContent;
}
