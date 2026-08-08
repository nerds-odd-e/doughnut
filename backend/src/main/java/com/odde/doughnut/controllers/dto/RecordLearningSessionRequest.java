package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class RecordLearningSessionRequest {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public Integer notebookId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public String reportMarkdown;
}
