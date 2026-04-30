package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Folder;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notebook root-level folder row for listing.")
public record NotebookRootFolder(
    @Schema(type = "integer") int id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug) {

  public static NotebookRootFolder from(Folder folder) {
    return new NotebookRootFolder(folder.getId(), folder.getName(), folder.getSlug());
  }
}
