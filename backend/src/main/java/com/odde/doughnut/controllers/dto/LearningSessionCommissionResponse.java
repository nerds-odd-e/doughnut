package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.LearningSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LearningSessionCommissionResponse {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int learningSessionId;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String requestMarkdown;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private LearningSessionStatus status;
}
