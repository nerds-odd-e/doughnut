package com.odde.donut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordedLearningSessionItem {
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private String noteTitle;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int grade;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private int memoryTrackerId;
}
