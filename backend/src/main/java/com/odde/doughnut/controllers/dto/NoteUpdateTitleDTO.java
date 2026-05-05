package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.validators.DisplayNamePathSeparators;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class NoteUpdateTitleDTO {
  @NotBlank
  @Size(max = Note.MAX_TITLE_LENGTH)
  @Pattern(regexp = DisplayNamePathSeparators.REGEXP, message = DisplayNamePathSeparators.MESSAGE)
  @Getter
  @Setter
  private String newTitle = "";
}
