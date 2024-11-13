package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AssistantCreationService;
import com.odde.doughnut.services.ai.AssistantRunService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public final class AiAdvisorWithStorageService {
  private final AiAssistantServiceFactory aiAssistantServiceFactory;
  private final ModelFactoryService modelFactoryService;

  public AiAdvisorWithStorageService(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      ModelFactoryService modelFactoryService) {
    this.aiAssistantServiceFactory = new AiAssistantServiceFactory(openAiApi);
    this.modelFactoryService = modelFactoryService;
  }

  public String createThread(AssistantService assistantService, Note note) {
    return assistantService.createThread(note, List.of());
  }

  public ChatAboutNoteService getChatAboutNoteService(
      String threadId, AssistantService assistantService) {
    return new ChatAboutNoteService(threadId, assistantService);
  }

  public String getChatAssistantIdForNotebook(Notebook notebook) {
    NotebookAssistant assistant = notebook.getNotebookAssistant();
    if (assistant != null) {
      return assistant.getAssistantId();
    }
    return getDefaultAssistantSettingAccessor().getValue();
  }

  public AssistantService getChatAssistantServiceForNotebook(Notebook notebook) {
    return aiAssistantServiceFactory.getAssistantService(getChatAssistantIdForNotebook(notebook));
  }

  private GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }

  private GlobalSettingsService.GlobalSettingsKeyValue getDefaultAssistantSettingAccessor() {
    return getGlobalSettingsService().defaultAssistantId();
  }

  public Assistant recreateDefaultAssistant(Timestamp currentUTCTimestamp) {
    String modelName = getGlobalSettingsService().globalSettingOthers().getValue();
    AssistantCreationService service = aiAssistantServiceFactory.getAssistantCreationService();
    Assistant assistant = service.createDefaultAssistant(modelName, "Note details completion");
    getDefaultAssistantSettingAccessor().setKeyValue(currentUTCTimestamp, assistant.getId());
    return assistant;
  }

  public NotebookAssistant recreateNotebookAssistant(
      Timestamp currentUTCTimestamp, User creator, Notebook notebook, String additionalInstruction)
      throws IOException {
    AssistantCreationService service = aiAssistantServiceFactory.getAssistantCreationService();
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
    NotebookAssistant notebookAssistant = notebook.getNotebookAssistant();
    if (notebookAssistant == null) {
      notebookAssistant = new NotebookAssistant();
      notebookAssistant.setNotebook(notebook);
    }
    notebookAssistant.setCreator(creator);
    notebookAssistant.setCreatedAt(currentUTCTimestamp);
    notebookAssistant.setAssistantId(chatAssistant.getId());
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
    AssistantService assistantService = getChatAssistantServiceForNotebook(note.getNotebook());

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

  public AssistantRunService getAssistantRunService(String threadId, String runId) {
    return aiAssistantServiceFactory.getAssistantRunService(threadId, runId);
  }
}
