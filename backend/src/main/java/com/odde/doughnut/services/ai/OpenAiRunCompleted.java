package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;

public final class OpenAiRunCompleted implements OpenAiRunResult {
  public OpenAiRunCompleted(OpenAiApiHandler openAiApiHandler, Run updatedRun, AiTool tool) {}
}
