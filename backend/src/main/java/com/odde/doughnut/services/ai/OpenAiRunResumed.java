package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;

public class OpenAiRunResumed extends OpenAiOngoingRun {

  public OpenAiRunResumed(
      OpenAiApiHandler openAiApiHandler, String threadId, String runId, AiTool tool) {
    super(openAiApiHandler, new Run(), tool);
    this.run.setId(runId);
    this.run.setThreadId(threadId);
  }
}
