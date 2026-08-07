package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DueCommissionedMemoryTrackerLite {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int memoryTrackerId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int notebookId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String notebookName;
}
