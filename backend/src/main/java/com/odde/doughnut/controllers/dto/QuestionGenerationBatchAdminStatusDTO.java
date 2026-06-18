package com.odde.doughnut.controllers.dto;

import java.sql.Timestamp;
import java.util.Map;
import lombok.Data;

@Data
public class QuestionGenerationBatchAdminStatusDTO {
  private Map<String, Long> batchCountsByStatus;
  private Map<String, Long> requestCountsByStatus;
  private boolean openAiTokenConfigured;
  private boolean prodProfileActive;
  private boolean schedulerActive;
  private Timestamp lastMaintenanceStartedAt;
  private Timestamp lastMaintenanceFinishedAt;
  private String lastMaintenanceError;
}
