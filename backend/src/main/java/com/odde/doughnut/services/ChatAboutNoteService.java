package com.odde.doughnut.services;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.AssistantThread;
import com.odde.doughnut.services.commands.GetAiStreamHelper;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ChatAboutNoteService {
  protected final NotebookAssistantForNoteService notebookAssistantForNoteService;
  protected final Note note;

  public ChatAboutNoteService(
      NotebookAssistantForNoteService notebookAssistantForNoteService, Note note) {
    this.notebookAssistantForNoteService = notebookAssistantForNoteService;
    this.note = note;
  }

  public ThreadStage startChat(Conversation conversation, ConversationService conversationService) {
    return new ThreadStage(conversation, conversationService, this);
  }

  public static class ThreadStage {
    private final Conversation conversation;
    private final ConversationService conversationService;
    private final ChatAboutNoteService service;

    private ThreadStage(
        Conversation conversation,
        ConversationService conversationService,
        ChatAboutNoteService service) {
      this.conversation = conversation;
      this.conversationService = conversationService;
      this.service = service;
    }

    public MessageStage createOrResumeThread() {
      AssistantThread thread;
      if (conversation.getAiAssistantThreadId() == null) {
        thread = service.notebookAssistantForNoteService.createThreadWithNoteInfo(List.of());
        conversationService.setConversationAiAssistantThreadId(conversation, thread.getThreadId());
      } else {
        thread =
            service.notebookAssistantForNoteService.getThread(
                conversation.getAiAssistantThreadId());
      }
      return new MessageStage(conversation, conversationService, thread, service);
    }
  }

  public static class MessageStage {
    private final Conversation conversation;
    private final ConversationService conversationService;
    private final AssistantThread thread;
    private final ChatAboutNoteService service;

    private MessageStage(
        Conversation conversation,
        ConversationService conversationService,
        AssistantThread thread,
        ChatAboutNoteService service) {
      this.conversation = conversation;
      this.conversationService = conversationService;
      this.thread = thread;
      this.service = service;
    }

    public FinalStage provideUnseenMessages() {
      Timestamp lastAiAssistantThreadSync = conversation.getLastAiAssistantThreadSync();
      if (lastAiAssistantThreadSync != null
          && service.note.getUpdatedAt().after(lastAiAssistantThreadSync)) {
        thread.createAssistantMessage(
            "The note content has been update:\n\n%s".formatted(service.note.getNoteDescription()));
      }
      List<ConversationMessage> unseen = conversation.getUnseenMessagesByAssistant();
      if (!unseen.isEmpty()) {
        thread.createUserMessage(GetAiStreamHelper.formatUnsentMessages(unseen));
      }
      conversationService.updateLastAiAssistantThreadSync(conversation);
      return new FinalStage(conversation, conversationService, thread);
    }
  }

  public static class FinalStage {
    private final Conversation conversation;
    private final ConversationService conversationService;
    private final AssistantThread thread;

    private FinalStage(
        Conversation conversation,
        ConversationService conversationService,
        AssistantThread thread) {
      this.conversation = conversation;
      this.conversationService = conversationService;
      this.thread = thread;
    }

    public SseEmitter getReplyStream() {
      return thread
          .withAdditionalInstructions(
              "User is seeking for having a conversation, so don't call functions to update the note unless user asks explicitly.")
          .runStream()
          .getSseEmitter(
              (message -> {
                String content = GetAiStreamHelper.extractMessageContent(message);
                conversationService.addMessageToConversation(conversation, null, content);
              }));
    }
  }
}
