package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;

public class OpenAiRunResumed extends OpenAiRun {

  public OpenAiRunResumed(
      OpenAiApiHandler openAiApiHandler, String threadId, String runId, AiTool tool) {
    super(openAiApiHandler, threadId, new Run(), tool);
    this.run.setId(runId);
  }
}
