package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.client.OpenAiApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
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
  public AiCompletionResponse getCompletion(
      @PathVariable(name = "note") Note note, @RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getAiCompletion(
        aiCompletionParams, note, getDefaultOpenAiChatModel(), getAssistantId());
  }

  @PostMapping("/{note}/answer-clarifying-question")
  public AiCompletionResponse answerCompletionClarifyingQuestion(
      @PathVariable(name = "note") Note note, @RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.answerAiCompletionClarifyingQuestion(
        aiCompletionParams, note, getDefaultOpenAiChatModel(), getAssistantId());
  }

  @PostMapping("/chat")
  public ChatResponse chat(
      @RequestParam(value = "note") Note note, @RequestBody ChatRequest request)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    String userMessage = request.getUserMessage();
    String assistantMessage =
        this.aiAdvisorService.chatWithAi(note, userMessage, getDefaultOpenAiChatModel());
    return new ChatResponse(assistantMessage);
  }

  @PostMapping("/generate-image")
  public AiGeneratedImage generateImage(@RequestBody String prompt) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(aiAdvisorService.getImage(prompt));
  }

  @GetMapping("/available-gpt-models")
  public List<String> getAvailableGptModels() {
    return aiAdvisorService.getAvailableGptModels();
  }

  @PostMapping("/recreate-all-assistants")
  public Map<String, String> recreateAllAssistants() throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    Map<String, String> result = new HashMap<>();
    Assistant noteCompletionAssistant =
        aiAdvisorService.createNoteCompletionAssistant(getDefaultOpenAiChatModel());
    String id = noteCompletionAssistant.getId();
    getGlobalSettingsService()
        .getNoteCompletionAssistantId()
        .setKeyValue(testabilitySettings.getCurrentUTCTimestamp(), id);

    result.put("note details completion", id);
    return result;
  }

  private String getDefaultOpenAiChatModel() {
    return getGlobalSettingsService().getGlobalSettingOthers().getValue();
  }

  private String getAssistantId() {
    return getGlobalSettingsService().getNoteCompletionAssistantId().getValue();
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
