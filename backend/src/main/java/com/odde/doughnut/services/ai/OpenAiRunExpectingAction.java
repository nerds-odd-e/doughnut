package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;

public class OpenAiRunExpectingAction extends OpenAiRun {

  public OpenAiRunExpectingAction(
      OpenAiApiHandler openAiApiHandler, String threadId, Run run, AiTool tool) {
    super(openAiApiHandler, threadId, run, tool);
  }

  public OpenAiRunExpectingAction(
      OpenAiApiHandler openAiApiHandler, String threadId, String runId, AiTool tool) {
    super(openAiApiHandler, threadId, new Run(), tool);
    this.run.setId(runId);
  }

  public OpenAiRunRequiredAction getToolCallResponse() {
    Run updatedRun = openAiApiHandler.retrieveUntilCompletedOrRequiresAction(threadId, run);
    if (updatedRun.getStatus().equals("requires_action")) {
      return new OpenAiRunRequiredAction(this.openAiApiHandler, updatedRun, tool);
    }
    return new OpenAiRunRequiredAction(this.openAiApiHandler, updatedRun, tool);
  }
}
