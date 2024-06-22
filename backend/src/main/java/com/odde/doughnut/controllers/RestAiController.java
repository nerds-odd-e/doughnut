package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.AssistantService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.client.OpenAiApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {

  private final AiAdvisorService aiAdvisorService;
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  public RestAiController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping("/{note}/completion")
  @Transactional
  public AiAssistantResponse getCompletion(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return getContentCompletionService()
        .createThreadAndRunWithFirstMessage(note, aiCompletionParams.getCompletionPrompt());
  }

  @PostMapping("/answer-clarifying-question")
  @Transactional
  public AiAssistantResponse answerCompletionClarifyingQuestion(
      @RequestBody AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    currentUser.assertLoggedIn();
    return getContentCompletionService()
        .answerAiCompletionClarifyingQuestion(answerClarifyingQuestionParams);
  }

  @PostMapping("/chat/{note}")
  @Transactional
  public AiAssistantResponse chat(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody ChatRequest request)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    if (request.getThreadId() == null) {
      return getChatService()
          .createThreadAndRunWithFirstMessageStream(note, request.getUserMessage());
    }
    return getChatService()
        .createMessageRunAndGetResponseStream(request.getUserMessage(), request.getThreadId());
  }

  @PostMapping("/generate-image")
  @Transactional
  public AiGeneratedImage generateImage(@RequestBody String prompt) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(aiAdvisorService.getOtherAiServices().getTimage(prompt));
  }

  @GetMapping("/available-gpt-models")
  public List<String> getAvailableGptModels() {
    return aiAdvisorService.getOtherAiServices().getAvailableGptModels();
  }

  @PostMapping("/recreate-all-assistants")
  @Transactional
  public Map<String, String> recreateAllAssistants() throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
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

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }

  private AssistantService getContentCompletionService() {
    return aiAdvisorService.getContentCompletionService(
        getGlobalSettingsService().noteCompletionAssistantId());
  }

  private AssistantService getChatService() {
    return aiAdvisorService.getChatService(getGlobalSettingsService().chatAssistantId());
  }
}
