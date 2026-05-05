package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookSettings;
import com.odde.doughnut.validators.DisplayNamePathSeparators;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotebookUpdateRequest {

  @JsonUnwrapped @Valid private NotebookSettings notebookSettings = new NotebookSettings();

  @Size(max = 500)
  private String description;

  @Size(max = Note.MAX_TITLE_LENGTH)
  @Pattern(regexp = DisplayNamePathSeparators.REGEXP, message = DisplayNamePathSeparators.MESSAGE)
  private String name;
}
