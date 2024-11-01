package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record AiAdvisorWithStorageService(
    AiAdvisorService aiAdvisorService, ModelFactoryService modelFactoryService) {
  public AiAdvisorWithStorageService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this(new AiAdvisorService(openAiApi), modelFactoryService);
  }

  private AssistantService getChatService(Note note) {
    NotebookAssistant assistant =
        modelFactoryService.notebookAssistantRepository.findByNotebook(note.getNotebook());
    if (assistant != null) {
      return aiAdvisorService.getContentCompletionService(assistant.getAssistantId());
    }
    return getContentCompletionService();
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }

  public AssistantService getContentCompletionService() {
    return aiAdvisorService.getContentCompletionService(
        getCompletionAssistantSettingAccessor().getValue());
  }

  private GlobalSettingsService.GlobalSettingsKeyValue getCompletionAssistantSettingAccessor() {
    return getGlobalSettingsService().noteCompletionAssistantId();
  }

  public Map<String, String> recreateAllAssistants(Timestamp currentUTCTimestamp) {
    Map<String, String> result = new HashMap<>();
    String modelName = getGlobalSettingsService().globalSettingOthers().getValue();
    Assistant completionAssistant = createCompletionAssistant(currentUTCTimestamp, modelName);
    result.put(completionAssistant.getName(), completionAssistant.getId());
    return result;
  }

  public Assistant createCompletionAssistant(Timestamp currentUTCTimestamp, String modelName) {
    AssistantService service = getContentCompletionService();
    Assistant assistant = service.createDefaultAssistant(modelName, "Note details completion");
    getCompletionAssistantSettingAccessor().setKeyValue(currentUTCTimestamp, assistant.getId());
    return assistant;
  }

  private String createThread(Note note, User user, AssistantService assistantService) {
    String threadId;
    threadId = assistantService.createThread(note);
    UserAssistantThread userAssistantThread = new UserAssistantThread();
    userAssistantThread.setThreadId(threadId);
    userAssistantThread.setNote(note);
    userAssistantThread.setUser(user);
    modelFactoryService().entityManager.persist(userAssistantThread);
    return threadId;
  }

  public List<Message> getMessageList(Note note, User entity) {
    UserAssistantThread byUserAndNote =
        modelFactoryService().userAssistantThreadRepository.findByUserAndNote(entity, note);
    if (byUserAndNote == null) {
      return List.of();
    }
    return getChatService(note).loadPreviousMessages(byUserAndNote.getThreadId());
  }

  public NotebookAssistant recreateNotebookAssistant(
      Timestamp currentUTCTimestamp, User creator, Notebook notebook, String additionalInstruction)
      throws IOException {
    AssistantService service = getContentCompletionService();
    String modelName = getGlobalSettingsService().globalSettingOthers().getValue();
    String fileContent = notebook.getNotebookDump();
    Assistant chatAssistant =
        service.createAssistantWithFile(
            modelName,
            "Assistant for notebook %s".formatted(notebook.getHeadNote().getTopicConstructor()),
            fileContent,
            additionalInstruction);
    return updateNotebookAssistant(currentUTCTimestamp, creator, notebook, chatAssistant);
  }

  private NotebookAssistant updateNotebookAssistant(
      Timestamp currentUTCTimestamp, User creator, Notebook notebook, Assistant chatAssistant) {
    NotebookAssistant notebookAssistant =
        this.modelFactoryService.notebookAssistantRepository.findByNotebook(notebook);
    if (notebookAssistant == null) {
      notebookAssistant = new NotebookAssistant();
      notebookAssistant.setNotebook(notebook);
    }
    notebookAssistant.setCreator(creator);
    notebookAssistant.setCreatedAt(currentUTCTimestamp);
    notebookAssistant.setAssistantId(chatAssistant.getId());
    this.modelFactoryService.save(notebookAssistant);
    return notebookAssistant;
  }

  public SseEmitter getAIReplySSE(Note note, String threadId, String userMessage) {
    AssistantService assistantService = getChatService(note);
    Flowable<AssistantSSE> runStream =
        assistantService.createMessageRunAndGetResponseStream(userMessage, threadId);
    SseEmitter emitter = new SseEmitter();
    runStream.subscribe(
        sse -> {
          try {
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name(sse.getEvent().eventName).data(sse.getData());
            emitter.send(builder);
            if (Objects.equals(sse.getEvent().eventName, "done")) {
              emitter.complete();
            }
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        });
    return emitter;
  }

  public String getOrCreateThread(Note note, User user, String threadId) {
    if (threadId != null) {
      return threadId;
    }
    AssistantService assistantService = getChatService(note);
    return createThread(note, user, assistantService);
  }
}
