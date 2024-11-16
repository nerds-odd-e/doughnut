package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;

public abstract class OpenAiRun {
  protected final OpenAiApiHandler openAiApiHandler;
  protected final Run run;
  protected final AiTool tool;

  public OpenAiRun(OpenAiApiHandler openAiApiHandler, Run run, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.run = run;
    this.tool = tool;
  }

  public OpenAiRunExpectingAction submitToolOutputs(String toolCallId, Object result)
      throws JsonProcessingException {
    Run currentRun =
        openAiApiHandler.submitToolOutputs(run.getThreadId(), run.getId(), toolCallId, result);
    return new OpenAiRunExpectingAction(openAiApiHandler, currentRun, tool);
  }

  public OpenAiRun cancelRun() {
    openAiApiHandler.cancelRun(run.getThreadId(), run.getId());
    return this;
  }

  public String getRunId() {
    return run.getId();
  }

  public abstract Object getFirstArgument() throws JsonProcessingException;

  public abstract ToolCallInfo getToolCallInfo();
}
