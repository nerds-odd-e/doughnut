package com.odde.doughnut.controllers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordLearningSessionResponse {
  private Timestamp recordedAt;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private List<RecordedLearningSessionItem> recordedItems = new ArrayList<>();

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private List<RejectedLearningSessionReportEntry> rejectedEntries = new ArrayList<>();
}
