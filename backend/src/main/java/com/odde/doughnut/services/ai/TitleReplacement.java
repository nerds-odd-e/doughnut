package com.odde.doughnut.services.ai;

import static com.odde.doughnut.entities.Note.NOTE_OF_CURRENT_FOCUS;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
