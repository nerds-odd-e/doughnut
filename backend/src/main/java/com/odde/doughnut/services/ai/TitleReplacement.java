package com.odde.doughnut.services.ai;

import static com.odde.doughnut.entities.Note.NOTE_OF_CURRENT_FOCUS;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@JsonClassDescription(
    "Generate a concise and accurate note title from the note content the model sees (including any hidden focus-context block) and pass it to the function for the user to update their note. The title should be a single word, a phrase, or at most one sentence that captures what the note is about. Prefer specificity over repeating notebook-wide themes. Keep the existing title if it's already correct and concise.")
@Data
public class TitleReplacement {
  @NotNull
  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @JsonPropertyDescription(
      "The title to be replaced for the "
          + NOTE_OF_CURRENT_FOCUS
          + ". Max size is "
          + Note.MAX_TITLE_LENGTH)
  @JsonProperty(required = true)
  public String newTitle;
}
