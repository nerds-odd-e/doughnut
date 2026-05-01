package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.RelationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor
@Data
public class NoteTopology {
  @NonNull private Integer id;

  private String title;
  private RelationType relationType;
  private NoteTopology targetNoteTopology;
  private NoteTopology parentOrSubjectNoteTopology;

  @NonNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer notebookId;

  private String notebookName;

  /** Present when the note is assigned to a folder (folder-first containment). */
  private Integer folderId;

  public int getId() {
    return this.id;
  }
}
