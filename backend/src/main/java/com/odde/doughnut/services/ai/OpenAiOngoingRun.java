package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;
import java.util.Map;

public abstract class OpenAiOngoingRun {
  protected final OpenAiApiHandler openAiApiHandler;
  protected final Run run;
  protected final AiTool tool;

  public OpenAiOngoingRun(OpenAiApiHandler openAiApiHandler, Run run, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.run = run;
    this.tool = tool;
  }

  public OpenAiRunExpectingAction submitToolOutputs(Map<String, ?> results)
      throws JsonProcessingException {
    Run currentRun = openAiApiHandler.submitToolOutputs(run.getThreadId(), run.getId(), results);
    return new OpenAiRunExpectingAction(openAiApiHandler, currentRun, tool);
  }

  public OpenAiOngoingRun cancelRun() {
    openAiApiHandler.cancelRun(run.getThreadId(), run.getId());
    return this;
  }

  public String getRunId() {
    return run.getId();
  }
}
