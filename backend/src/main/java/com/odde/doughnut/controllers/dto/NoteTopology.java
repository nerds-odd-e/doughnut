package com.odde.doughnut.controllers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor
@Data
public class NoteTopology {
  @NonNull private Integer id;

  private String title;

  /** Present when the note is assigned to a folder (folder-first containment). */
  private Integer folderId;

  public int getId() {
    return this.id;
  }
}
