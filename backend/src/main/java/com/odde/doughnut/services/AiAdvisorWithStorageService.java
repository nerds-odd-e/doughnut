package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.ToolCallResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.message.Message;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
@Service
public final class AiAdvisorWithStorageService {
  @Getter private final AiAdvisorService aiAdvisorService;
  private final ModelFactoryService modelFactoryService;

  public String createThread(AssistantService assistantService, Note note) {
    return assistantService.createThread(note);
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

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService) {
    validateConversationForAiReply(conversation);
    ChatAboutNoteService chatService =
        setupChatServiceForConversation(conversation, conversationService);
    setupMessageHandler(conversation, chatService, conversationService);
    return chatService.getAIReplySSE();
  }

  private void validateConversationForAiReply(Conversation conversation) {
    Note note = conversation.getSubject().getNote();
    if (note == null) {
      throw new RuntimeException("Only note related conversation can have AI reply");
    }
  }

  private ChatAboutNoteService setupChatServiceForConversation(
      Conversation conversation, ConversationService conversationService) {
    Note note = conversation.getSubject().getNote();
    String threadId = conversation.getAiAssistantThreadId();
    AssistantService assistantService = getChatAssistantService(note);

    if (threadId == null) {
      threadId = createThread(assistantService, note);
      conversationService.setConversationAiAssistantThreadId(conversation, threadId);
    }

    ChatAboutNoteService chatService = getChatAboutNoteService(threadId, assistantService);
    chatService.sendNoteUpdateMessageIfNeeded(note, conversation);
    sendUnsentMessagesToAI(conversation, chatService);
    conversationService.updateLastAiAssistantThreadSync(conversation);

    return chatService;
  }

  private void sendUnsentMessagesToAI(Conversation conversation, ChatAboutNoteService chatService) {
    List<ConversationMessage> unsynced =
        conversation.getConversationMessages().stream()
            .filter(
                msg ->
                    conversation.getLastAiAssistantThreadSync() == null
                        || msg.getCreatedAt().after(conversation.getLastAiAssistantThreadSync()))
            .filter(msg -> msg.getSender() != null)
            .toList();

    if (!unsynced.isEmpty()) {
      String combinedMessage = formatUnsentMessages(unsynced);
      chatService.createUserMessage(combinedMessage);
    } else {
      chatService.createUserMessage("just say something.");
    }
  }

  private String formatUnsentMessages(List<ConversationMessage> messages) {
    StringBuilder combined = new StringBuilder();
    for (ConversationMessage msg : messages) {
      combined.append(String.format("user `%s` says:%n", msg.getSender().getName()));
      combined.append("-----------------\n");
      combined.append(msg.getMessage());
      combined.append("\n\n");
    }
    return combined.toString();
  }

  private void setupMessageHandler(
      Conversation conversation,
      ChatAboutNoteService chatService,
      ConversationService conversationService) {
    chatService.onMessageCompleted(
        message -> {
          String content = extractMessageContent(message);
          conversationService.addMessageToConversation(conversation, null, content);
        });
  }

  private static String extractMessageContent(Message message) {
    return message.getContent().stream()
        .filter(c -> "text".equals(c.getType()))
        .map(c -> c.getText().getValue())
        .findFirst()
        .orElse("");
  }

  public void submitToolOutputs(
      String threadId, String runId, String toolCallId, ToolCallResult result)
      throws JsonProcessingException {
    aiAdvisorService.submitToolOutputs(threadId, runId, toolCallId, result);
  }
}
