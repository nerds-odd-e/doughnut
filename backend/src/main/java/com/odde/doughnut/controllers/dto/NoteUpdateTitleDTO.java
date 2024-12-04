package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class NoteUpdateTitleDTO {
  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  @NotNull
  private String newTitle = "";
}
