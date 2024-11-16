package com.odde.doughnut.services.ai;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToolCallInfo {
  private String threadId;

  private String runId;

  private String toolCallId;
}
