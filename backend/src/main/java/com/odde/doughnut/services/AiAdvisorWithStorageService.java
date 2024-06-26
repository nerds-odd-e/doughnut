package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.ChatRequest;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record AiAdvisorWithStorageService(
    AiAdvisorService aiAdvisorService, ModelFactoryService modelFactoryService) {
  public AiAdvisorWithStorageService(OpenAiApi openAiApi, ModelFactoryService modelFactoryService) {
    this(new AiAdvisorService(openAiApi), modelFactoryService);
  }

  private AssistantService getDefaultChatService() {
    return aiAdvisorService.getChatService(getDefaultChatAssistantSettingAccessor().getValue());
  }

  private GlobalSettingsService.GlobalSettingsKeyValue getDefaultChatAssistantSettingAccessor() {
    return getGlobalSettingsService().chatAssistantId();
  }

  private AssistantService getChatService(Note note) {
    NotebookAssistant assistant =
        modelFactoryService.notebookAssistantRepository.findByNotebook(note.getNotebook());
    if (assistant != null) {
      return aiAdvisorService.getChatService(assistant.getAssistantId());
    }
    return getDefaultChatService();
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
    Assistant chatAssistant = createChatAssistant(currentUTCTimestamp, modelName);
    result.put(chatAssistant.getName(), chatAssistant.getId());
    return result;
  }

  public Assistant createCompletionAssistant(Timestamp currentUTCTimestamp, String modelName) {
    AssistantService service = getContentCompletionService();
    Assistant assistant = service.createAssistant(modelName, "Note details completion");
    getCompletionAssistantSettingAccessor().setKeyValue(currentUTCTimestamp, assistant.getId());
    return assistant;
  }

  public Assistant createChatAssistant(Timestamp currentUTCTimestamp, String modelName) {
    AssistantService service = getDefaultChatService();
    Assistant chatAssistant = service.createAssistant(modelName, "chat assistant");
    getDefaultChatAssistantSettingAccessor()
        .setKeyValue(currentUTCTimestamp, chatAssistant.getId());
    return chatAssistant;
  }

  public Flowable<AssistantSSE> getChatMessages(Note note, ChatRequest request, User user) {
    AssistantService assistantService = getChatService(note);
    String threadId = request.getThreadId();
    if (threadId == null) {
      threadId = assistantService.createThread(note);
      UserAssistantThread userAssistantThread = new UserAssistantThread();
      userAssistantThread.setThreadId(threadId);
      userAssistantThread.setNote(note);
      userAssistantThread.setUser(user);
      modelFactoryService().entityManager.persist(userAssistantThread);
    }
    return assistantService.createMessageRunAndGetResponseStream(
        request.getUserMessage(), threadId);
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
      Timestamp currentUTCTimestamp, User creator, Notebook notebook) {
    NotebookAssistant notebookAssistant = new NotebookAssistant();
    notebookAssistant.setNotebook(notebook);
    notebookAssistant.setCreator(creator);
    notebookAssistant.setCreatedAt(currentUTCTimestamp);
    notebookAssistant.setAssistantId("assistant-id-1");
    this.modelFactoryService.save(notebookAssistant);
    return notebookAssistant;
  }
}
