package com.odde.doughnut.services;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.theokanning.openai.client.OpenAiApi;

public record AiAdvisorWithStorageService(
    AiAdvisorService aiAdvisorService, ModelFactoryService modelFactoryService) {
  public AiAdvisorWithStorageService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this(new AiAdvisorService(openAiApi), modelFactoryService);
  }
}
