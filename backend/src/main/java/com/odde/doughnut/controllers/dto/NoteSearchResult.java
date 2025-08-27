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

  private Float distance;

  public NoteTopology getNoteTopology() {
    return this.noteTopology;
  }

  public Float getDistance() {
    return this.distance;
  }
}
