package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;
import lombok.Getter;

public abstract class OpenAiRun {
  protected final OpenAiApiHandler openAiApiHandler;
  @Getter protected final String threadId;
  protected final Run run;
  protected final AiTool tool;

  public OpenAiRun(OpenAiApiHandler openAiApiHandler, String threadId, Run run, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.threadId = threadId;
    this.run = run;
    this.tool = tool;
  }

  public OpenAiRunExpectingAction submitToolOutputs(String toolCallId, Object result)
      throws JsonProcessingException {
    Run currentRun = openAiApiHandler.submitToolOutputs(threadId, run.getId(), toolCallId, result);
    return new OpenAiRunExpectingAction(openAiApiHandler, currentRun, tool);
  }

  public OpenAiRun cancelRun() {
    openAiApiHandler.cancelRun(threadId, run.getId());
    return this;
  }

  public String getRunId() {
    return run.getId();
  }

  public abstract Object getFirstArgument() throws JsonProcessingException;

  public abstract ToolCallInfo getToolCallInfo();
}
