package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class McpNoteAddDTO {
  @Getter @Setter public String parentNote;
  @Getter @Setter public NoteCreationDTO noteCreationDTO;
}
