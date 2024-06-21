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
        .initiateAThread(note, aiCompletionParams.getCompletionPrompt());
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
  public ChatResponse chat(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody ChatRequest request)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    String userMessage = request.getUserMessage();
    GlobalSettingsService.GlobalSettingsKeyValue settingAccessor =
        getGlobalSettingsService().chatAssistantId();
    String assistantMessage =
        this.aiAdvisorService
            .getChatService(settingAccessor)
            .initiateAThread(note, userMessage)
            .getLastMessage();
    return new ChatResponse(assistantMessage);
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
    GlobalSettingsService globalSettingsService = getGlobalSettingsService();
    Map<String, String> result = new HashMap<>();
    String modelName = globalSettingsService.globalSettingOthers().getValue();
    String completionAssistant =
        getContentCompletionService().createAssistant(modelName, "Note details completion").getId();
    result.put("note details completion", completionAssistant);
    globalSettingsService
        .noteCompletionAssistantId()
        .setKeyValue(currentUTCTimestamp, completionAssistant);
    String chatAssistant = getChatService().createAssistant(modelName, "Chat assistant").getId();
    result.put("chat assistant", chatAssistant);
    globalSettingsService.chatAssistantId().setKeyValue(currentUTCTimestamp, chatAssistant);
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
