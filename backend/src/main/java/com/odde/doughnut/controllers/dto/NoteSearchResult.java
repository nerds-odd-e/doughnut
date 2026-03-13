package com.odde.doughnut.controllers.dto;

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

  @Schema(description = "Title of the notebook this result belongs to")
  private String notebookTitle;

  private Float distance;

  public NoteSearchResult(NoteTopology noteTopology, Float distance) {
    this.noteTopology = noteTopology;
    this.notebookId = noteTopology != null ? noteTopology.getNotebookId() : null;
    this.notebookTitle = noteTopology != null ? noteTopology.getNotebookTitle() : null;
    this.distance = distance;
  }

  public NoteTopology getNoteTopology() {
    return this.noteTopology;
  }

  public Integer getNotebookId() {
    return this.notebookId;
  }

  public String getNotebookTitle() {
    return this.notebookTitle;
  }

  public Float getDistance() {
    return this.distance;
  }
}
