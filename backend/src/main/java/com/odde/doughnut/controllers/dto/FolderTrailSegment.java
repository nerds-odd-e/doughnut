package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Folder;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Folder id and display name: ancestor breadcrumb segments (outermost first), notebook root"
            + " listing rows, or direct child folders in a folder listing.")
public record FolderTrailSegment(
    @Schema(type = "integer") int id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name) {

  public static FolderTrailSegment from(Folder folder) {
    return new FolderTrailSegment(folder.getId(), folder.getName());
  }
}
