package com.odde.doughnut.controllers.dto;

import lombok.Getter;

public class ExtractPointToChildResponseDTO {
  @Getter private final NoteRealm createdNote;
  @Getter private final NoteRealm updatedParentNote;

  public ExtractPointToChildResponseDTO(NoteRealm created, NoteRealm parent) {
    this.createdNote = created;
    this.updatedParentNote = parent;
  }
}
