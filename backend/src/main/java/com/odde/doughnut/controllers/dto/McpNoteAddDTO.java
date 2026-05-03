package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class McpNoteAddDTO {
  /**
   * Title search key; resolves to an anchor note whose folder placement guides creation—not a
   * structural parent FK.
   */
  @Getter @Setter public String parentNote;

  @Getter @Setter public NoteCreationDTO noteCreationDTO;
}
