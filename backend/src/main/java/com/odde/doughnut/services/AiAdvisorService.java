package com.odde.doughnut.services;

import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
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

  public AssistantService getContentCompletionService(String assistantId) {
    return new AssistantService(
        openAiApiHandler, assistantId, AiToolFactory.getCompletionAiTools());
  }
}
