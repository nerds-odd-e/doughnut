package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.assistant.Assistant;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public final class AiAdvisorWithStorageService {
  @Getter private final AiAdvisorService aiAdvisorService;
  private final ModelFactoryService modelFactoryService;

  public String getExistingThreadId(User user, Note note) {
    UserAssistantThread byUserAndNote =
        modelFactoryService.userAssistantThreadRepository.findByUserAndNote(user, note);
    String threadId = null;
    if (byUserAndNote != null) {
      threadId = byUserAndNote.getThreadId();
    }
    return threadId;
  }

  public String createThread(User user, AssistantService assistantService, Note note) {
    String threadId = assistantService.createThread(note);
    UserAssistantThread userAssistantThread = new UserAssistantThread();
    userAssistantThread.setThreadId(threadId);
    userAssistantThread.setNote(note);
    userAssistantThread.setUser(user);
    modelFactoryService.entityManager.persist(userAssistantThread);
    return threadId;
  }

  public ChatAboutNoteService getChatAboutNoteService(
      String threadId, AssistantService assistantService) {
    return new ChatAboutNoteService(threadId, assistantService, modelFactoryService);
  }

  public AssistantService getChatAssistantService(Note note) {
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
}
