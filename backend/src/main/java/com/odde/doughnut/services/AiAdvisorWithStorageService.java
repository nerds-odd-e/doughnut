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
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

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

  public String getCompletionAiOpinion(List<ConversationDetail> conversationDetails) {
    if (CollectionUtils.isEmpty(conversationDetails)) {
      return StringUtils.EMPTY;
    }
    String prompt = buildAiPrompt(conversationDetails);
    List<Message> response = fetchAiResponse(prompt);
    return getAiOpinionMessage(response);
  }

  private List<Message> fetchAiResponse(String prompt) {
    return getContentCompletionService().createThreadAndRunWithFirstMessage(prompt).getMessages();
  }

  private String getAiOpinionMessage(List<Message> response) {
    if (!CollectionUtils.isEmpty(response)
        && !CollectionUtils.isEmpty(response.getFirst().getContent())
        && response.getFirst().getContent().getFirst().getText() != null
        && StringUtils.isNotBlank(
            response.getFirst().getContent().getFirst().getText().getValue())) {
      return response.getFirst().getContent().getFirst().getText().getValue();
    } else {
      return StringUtils.EMPTY;
    }
  }

  private String buildAiPrompt(List<ConversationDetail> conversationDetails) {
    StringBuilder prompt = new StringBuilder();
    for (ConversationDetail detail : conversationDetails) {
      User conversationDetailOwner = detail.getConversationDetailInitiator();
      String userChatId =
          Objects.isNull(conversationDetailOwner)
              ? "AI"
              : conversationDetailOwner.getId().toString();
      prompt.append(userChatId).append(": ").append(detail.getMessage()).append("\n");
    }
    return prompt.toString();
  }
}
