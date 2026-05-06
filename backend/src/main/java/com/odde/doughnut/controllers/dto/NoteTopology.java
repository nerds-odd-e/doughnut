package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import java.sql.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor
@Data
public class NoteTopology {
  @NonNull private Integer id;

  @NotBlank private String title;

  /** Present when the note is assigned to a folder (folder-first containment). */
  private Integer folderId;

  private Timestamp createdAt;

  private Timestamp updatedAt;

  public int getId() {
    return this.id;
  }
}
