package com.odde.donut.controllers.dto;

import lombok.Value;

@Value
public class QuestionGenerationBatchSubmissionSummaryDTO {
  int consideredUserCount;
  int submittedCount;
  int failedCount;
  int skippedCount;
}
