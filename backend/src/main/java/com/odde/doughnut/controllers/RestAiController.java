package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorWithStorageService;
import com.odde.doughnut.services.ChatAboutNoteService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.assistants.message.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {

  private final UserModel currentUser;

  @Resource(name = "testabilitySettings")
  private final TestabilitySettings testabilitySettings;

  private final AiAdvisorWithStorageService aiAdvisorWithStorageService;

  public RestAiController(
      AiAdvisorWithStorageService aiAdvisorWithStorageService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.aiAdvisorWithStorageService = aiAdvisorWithStorageService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
  }

  @PostMapping("/{note}/completion")
  @Transactional
  public AiAssistantResponse getCompletion(
      @PathVariable(name = "note") @Schema(type = "integer") Note note,
      @RequestBody AiCompletionParams aiCompletionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorWithStorageService
        .getContentCompletionService()
        .createThreadAndRunWithFirstMessage(note, aiCompletionParams.getCompletionPrompt());
  }

  @PostMapping("/answer-clarifying-question")
  @Transactional
  public AiAssistantResponse answerCompletionClarifyingQuestion(
      @RequestBody AiCompletionAnswerClarifyingQuestionParams answerClarifyingQuestionParams) {
    currentUser.assertLoggedIn();
    return aiAdvisorWithStorageService
        .getContentCompletionService()
        .answerAiCompletionClarifyingQuestion(answerClarifyingQuestionParams);
  }

  @GetMapping("/chat/{note}")
  public List<Message> tryRestoreChat(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    String threadId =
        aiAdvisorWithStorageService.getExistingThreadId(currentUser.getEntity(), note);
    if (threadId == null) {
      return List.of();
    }
    return aiAdvisorWithStorageService.getChatService(note, threadId).getMessageList();
  }

  @PostMapping(path = "/chat/{note}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Transactional
  public SseEmitter chat(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody ChatRequest request)
      throws UnexpectedNoAccessRightException {
    currentUser.assertReadAuthorization(note);
    ChatAboutNoteService chatService =
        aiAdvisorWithStorageService.getChatService(note, request.getThreadId());
    chatService.createUserMessage(request.getUserMessage());
    return chatService.getAIReplySSE();
  }

  @GetMapping("/dummy")
  public DummyForGeneratingTypes dummyEntryToGenerateDataTypesThatAreRequiredInEventStream()
      throws HttpMediaTypeNotAcceptableException {
    throw new HttpMediaTypeNotAcceptableException("dummy");
  }

  @PostMapping("/generate-image")
  @Transactional
  public AiGeneratedImage generateImage(@RequestBody String prompt) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(
        aiAdvisorWithStorageService.getAiAdvisorService().getOtherAiServices().getTimage(prompt));
  }

  @GetMapping("/available-gpt-models")
  public List<String> getAvailableGptModels() {
    return aiAdvisorWithStorageService
        .getAiAdvisorService()
        .getOtherAiServices()
        .getAvailableGptModels();
  }

  @PostMapping("/recreate-all-assistants")
  @Transactional
  public Map<String, String> recreateAllAssistants() throws UnexpectedNoAccessRightException {
    currentUser.assertAdminAuthorization();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return aiAdvisorWithStorageService.recreateAllAssistants(currentUTCTimestamp);
  }

  @PostMapping("/recreate-notebook-assistant/{notebook}")
  @Transactional
  public NotebookAssistant recreateNotebookAssistant(
      @PathVariable(value = "notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody NotebookAssistantCreationParams notebookAssistantCreationParams)
      throws UnexpectedNoAccessRightException, IOException {
    currentUser.assertAdminAuthorization();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return aiAdvisorWithStorageService.recreateNotebookAssistant(
        currentUTCTimestamp,
        currentUser.getEntity(),
        notebook,
        notebookAssistantCreationParams.getAdditionalInstruction());
  }
}
