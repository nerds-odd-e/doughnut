package com.odde.doughnut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
@AllArgsConstructor
public class NoteCreationResult {
  @NonNull private final NoteRealm created;
  @NonNull private final NoteRealm parent;

  public NoteRealm getCreated() {
    return this.created;
  }
}
