package com.odde.doughnut.services;

import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.AssistantRunService;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiAdvisorService {
  protected final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(@Qualifier("testableOpenAiApi") OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public OtherAiServices getOtherAiServices() {
    return new OtherAiServices(openAiApiHandler);
  }

  public AssistantService getAssistantService(String assistantId) {
    return new AssistantService(openAiApiHandler, assistantId);
  }

  public AssistantRunService getAssistantRunService(String threadId, String runId) {
    return new AssistantRunService(openAiApiHandler, threadId, runId);
  }

  public AssistantCreationService getAssistantCreationService() {
    return new AssistantCreationService(openAiApiHandler, AiToolFactory.getAllAssistantTools());
  }
}
