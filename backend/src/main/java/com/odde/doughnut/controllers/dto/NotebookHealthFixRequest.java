package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotebookHealthFixRequest {
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Must be true to bulk-purge fully empty folder trees")
  private Boolean removeEmptyFolders;
}
