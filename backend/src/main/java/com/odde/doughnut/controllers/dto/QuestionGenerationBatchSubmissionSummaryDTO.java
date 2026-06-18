package com.odde.doughnut.controllers.dto;

import lombok.Value;

@Value
public class QuestionGenerationBatchSubmissionSummaryDTO {
  int consideredUserCount;
  int submittedCount;
  int failedCount;
  int skippedCount;
}
