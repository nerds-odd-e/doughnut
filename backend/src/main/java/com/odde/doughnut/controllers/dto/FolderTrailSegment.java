package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Folder;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "One folder on the path from notebook root to the note's containing folder (outermost first).")
public record FolderTrailSegment(
    @Schema(type = "integer") int id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {

  public static FolderTrailSegment from(Folder folder) {
    return new FolderTrailSegment(folder.getId(), folder.getName());
  }
}
