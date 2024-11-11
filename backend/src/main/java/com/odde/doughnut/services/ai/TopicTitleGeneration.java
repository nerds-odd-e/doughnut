package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TopicTitleGeneration {
  @NotNull
  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @JsonPropertyDescription("The topic title for the note. Max size is " + Note.MAX_TITLE_LENGTH)
  public String topic;
}
