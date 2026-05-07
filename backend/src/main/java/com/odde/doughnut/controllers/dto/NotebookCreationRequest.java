package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotebookCreationRequest extends NoteUpdateTitleDTO {

  @Size(max = 500)
  private String description;
}
