package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.ToolCallResult;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.run.Run;
import com.theokanning.openai.client.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiAdvisorService {
  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public OtherAiServices getOtherAiServices() {
    return new OtherAiServices(openAiApiHandler);
  }

  public AssistantService getChatService(String assistantId) {
    return new AssistantService(
        openAiApiHandler, assistantId, AiToolFactory.getCompletionAiTools());
  }

  public void submitToolOutputs(
      String threadId, String runId, String toolCallId, ToolCallResult result)
      throws JsonProcessingException {
    openAiApiHandler.submitToolOutputs(threadId, runId, toolCallId, result);
  }

  public Run cancelRun(String threadId, String runId) {
    return openAiApiHandler.cancelRun(threadId, runId);
  }
}
