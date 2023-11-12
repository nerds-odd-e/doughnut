package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.controllers.json.CurrentModelVersionResponse;
import com.odde.doughnut.controllers.json.ModelVersionOption;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.AiModelService;
import com.theokanning.openai.OpenAiApi;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {

  private final AiAdvisorService aiAdvisorService;
  private final ModelFactoryService modelFactoryService;
  private final AiModelService aiModelService;
  private UserModel currentUser;

  public RestAiController(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService,
      UserModel currentUser) {
    this.aiAdvisorService = new AiAdvisorService(openAiApi);
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.aiModelService = new AiModelService(modelFactoryService);
  }

  @PostMapping("/{note}/completion")
  public AiCompletion getCompletion(
      @PathVariable(name = "note") Note note, @RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorService.getAiCompletion(aiCompletionParams, note);
  }

  @PostMapping("/chat")
  public ChatResponse chat(
      @RequestParam(value = "note") Note note, @RequestBody ChatRequest request)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    String userMessage = request.getUserMessage();
    String assistantMessage = this.aiAdvisorService.chatToAi(note, userMessage);
    return new ChatResponse(assistantMessage);
  }

  @PostMapping("/generate-image")
  public AiGeneratedImage generateImage(@RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(aiAdvisorService.getImage(aiCompletionParams.prompt));
  }

  @PostMapping("/trigger-finetuning/{fileId}")
  public ApiResponse triggerFineTune(@PathVariable(name = "fileId") String fileId) {
    currentUser.assertLoggedIn();

    try {
      aiAdvisorService.triggerFineTune(fileId);
    } catch (Exception e) {
      System.out.println(e.toString());
      return new ApiResponse("Failed");
    }

    return new ApiResponse("Successful");
  }

  @GetMapping("/model-versions")
  public List<ModelVersionOption> getModelVersions() {
    return aiAdvisorService.getModelVersions();
  }

  @GetMapping("/current-model-version")
  public CurrentModelVersionResponse getCurrentModelVersions() {
    return aiModelService.getCurrentModelVersions();
  }

  @PostMapping("/current-model-version")
  public CurrentModelVersionResponse setCurrentModelVersions(
      @RequestBody CurrentModelVersionResponse models) {
    return aiModelService.setCurrentModelVersions(models);
  }
}
