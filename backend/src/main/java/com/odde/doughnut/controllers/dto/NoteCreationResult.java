package com.odde.doughnut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Data
@AllArgsConstructor
public class NoteCreationResult {
  @NonNull private final NoteRealm created;
  @Nullable private final NoteRealm parent;

  public NoteRealm getCreated() {
    return this.created;
  }
}
