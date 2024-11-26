package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;

public class OpenAiRunExpectingAction {

  protected final OpenAiApiHandler openAiApiHandler;
  protected final Run run;
  protected final AiTool tool;

  public OpenAiRunExpectingAction(OpenAiApiHandler openAiApiHandler, Run run, AiTool tool) {
    this.openAiApiHandler = openAiApiHandler;
    this.run = run;
    this.tool = tool;
  }

  public OpenAiRun getToolCallRequiredAction() {
    Run updatedRun =
        openAiApiHandler.retrieveUntilCompletedOrRequiresAction(run.getThreadId(), run);
    if (updatedRun.getStatus().equals("requires_action")) {
      return new OpenAiRunRequiredAction(this.openAiApiHandler, updatedRun, tool);
    }
    return new OpenAiRunResumed(
        this.openAiApiHandler, updatedRun.getThreadId(), updatedRun.getId(), tool);
  }
}
