package com.odde.doughnut.controllers.dto;

import lombok.Getter;

public class PromotePointResponseDTO {
  @Getter private final NoteRealm createdNote;
  @Getter private final NoteRealm updatedParentNote;

  public PromotePointResponseDTO(NoteRealm created, NoteRealm parent) {
    this.createdNote = created;
    this.updatedParentNote = parent;
  }
}
