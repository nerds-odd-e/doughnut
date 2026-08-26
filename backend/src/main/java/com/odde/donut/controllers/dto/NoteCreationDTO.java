package com.odde.donut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class NoteCreationDTO extends NoteUpdateTitleDTO {
  @Getter @Setter private Integer folderId;

  @Getter @Setter private String content;
}
