package com.odde.doughnut.controllers.dto;

import java.util.Map;
import lombok.Data;

@Data
public class QuestionGenerationBatchAdminStatusDTO {
  private Map<String, Long> batchCountsByStatus;
  private Map<String, Long> requestCountsByStatus;
  private boolean openAiTokenConfigured;
  private boolean schedulerActive;
}
