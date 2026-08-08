package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AwaitingReportLearningSessionLite {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int notebookId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String notebookName;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int learningSessionId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String requestMarkdown;
}
