package com.odde.doughnut.services;

import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public record AiAdvisorWithStorageService(
    AiAdvisorService aiAdvisorService, ModelFactoryService modelFactoryService) {
  public AiAdvisorWithStorageService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this(new AiAdvisorService(openAiApi), modelFactoryService);
  }

  public AssistantService getChatService() {
    return aiAdvisorService.getChatService(getGlobalSettingsService().chatAssistantId());
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }

  public AssistantService getContentCompletionService() {
    return aiAdvisorService.getContentCompletionService(
        getGlobalSettingsService().noteCompletionAssistantId());
  }

  public Map<String, String> recreateAllAssistants(Timestamp currentUTCTimestamp) {
    Map<String, String> result = new HashMap<>();
    String modelName = getGlobalSettingsService().globalSettingOthers().getValue();
    AssistantService completionService = getContentCompletionService();
    result.put(
        completionService.assistantName(),
        completionService.createAssistant(modelName, currentUTCTimestamp));
    AssistantService chatService = getChatService();
    result.put(
        chatService.assistantName(), chatService.createAssistant(modelName, currentUTCTimestamp));
    return result;
  }
}
