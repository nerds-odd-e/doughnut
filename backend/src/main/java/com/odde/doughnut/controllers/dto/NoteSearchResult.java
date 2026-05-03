package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Note;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteSearchResult {
  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private NoteTopology noteTopology;

  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer notebookId;

  @Schema(description = "Name of the notebook this result belongs to")
  private String notebookName;

  private Float distance;

  public NoteSearchResult(Note note, Float distance) {
    NoteTopology topology = note != null ? note.getNoteTopology() : null;
    this.noteTopology = topology;
    this.notebookId =
        note != null && note.getNotebook() != null ? note.getNotebook().getId() : null;
    this.notebookName =
        note != null && note.getNotebook() != null ? note.getNotebook().getName() : null;
    this.distance = distance;
  }

  public NoteTopology getNoteTopology() {
    return this.noteTopology;
  }

  public Integer getNotebookId() {
    return this.notebookId;
  }

  public String getNotebookName() {
    return this.notebookName;
  }

  public Float getDistance() {
    return this.distance;
  }
}
