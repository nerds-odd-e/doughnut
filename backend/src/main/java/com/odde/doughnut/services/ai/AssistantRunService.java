package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.ToolCallResult;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import lombok.Getter;

public class AssistantRunService {
  protected final OpenAiApiHandler openAiApiHandler;
  private final String threadId;
  @Getter private final String runId;

  public AssistantRunService(OpenAiApiHandler openAiApiHandler, String threadId, String runId) {
    this.openAiApiHandler = openAiApiHandler;
    this.threadId = threadId;
    this.runId = runId;
  }

  public void submitToolOutputs(String toolCallId, ToolCallResult result)
      throws JsonProcessingException {
    openAiApiHandler.submitToolOutputs(threadId, runId, toolCallId, result);
  }

  public void cancelRun() {
    openAiApiHandler.cancelRun(threadId, runId);
  }
}
