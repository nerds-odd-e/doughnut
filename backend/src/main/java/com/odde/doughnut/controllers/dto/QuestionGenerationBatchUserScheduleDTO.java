package com.odde.doughnut.controllers.dto;

import java.sql.Timestamp;
import lombok.Data;

@Data
public class QuestionGenerationBatchUserScheduleDTO {
  private Timestamp nextScheduledAt;
  private String reason;
}
