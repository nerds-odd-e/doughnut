package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;
import lombok.Getter;

public class OpenAiRunExpectingAction {
  private final OpenAiApiHandler openAiApiHandler;
  @Getter private final String threadId;
  private final Run run;
  private final AiTool tool;

  public OpenAiRunExpectingAction(
      OpenAiApiHandler openAiApiHandler, String threadId, Run run, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.threadId = threadId;
    this.run = run;
    this.tool = tool;
  }

  public OpenAiRunExpectingAction(
      OpenAiApiHandler openAiApiHandler, String threadId, String runId, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.threadId = threadId;
    this.tool = tool;
    this.run = new Run();
    this.run.setId(runId);
  }

  public OpenAiRunExpectingAction submitToolOutputs(String toolCallId, Object result)
      throws JsonProcessingException {
    Run currentRun = openAiApiHandler.submitToolOutputs(threadId, run.getId(), toolCallId, result);
    return new OpenAiRunExpectingAction(openAiApiHandler, threadId, currentRun, tool);
  }

  public void cancelRun() {
    openAiApiHandler.cancelRun(threadId, run.getId());
  }

  public AiAssistantResponse getToolCallResponse() {
    Run updatedRun = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, run);
    if (updatedRun.getStatus().equals("requires_action")) {}
    return new AiAssistantResponse(this.openAiApiHandler, updatedRun, tool);
  }

  public String getRunId() {
    return run.getId();
  }
}
