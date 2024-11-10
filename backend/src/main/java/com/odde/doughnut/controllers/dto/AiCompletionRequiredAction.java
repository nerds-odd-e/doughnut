package com.odde.doughnut.controllers.dto;

import lombok.Data;

@Data
public class AiCompletionRequiredAction {
  private String contentToAppend;
  private String toolCallId;
  private String topicTitle;
}
