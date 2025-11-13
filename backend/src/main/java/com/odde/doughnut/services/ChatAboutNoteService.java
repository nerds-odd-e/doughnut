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

  private abstract static class BaseStage {
    protected final Conversation conversation;
    protected final ConversationService conversationService;
    protected final AssistantThread thread;

    protected BaseStage(
        Conversation conversation,
        ConversationService conversationService,
        AssistantThread thread) {
      this.conversation = conversation;
      this.conversationService = conversationService;
      this.thread = thread;
    }
  }

  public static class ThreadStage extends BaseStage {
    private final ChatAboutNoteService service;

    private ThreadStage(
        Conversation conversation,
        ConversationService conversationService,
        ChatAboutNoteService service) {
      super(conversation, conversationService, null);
      this.service = service;
    }

    public MessageStage createOrResumeThread() {
      AssistantThread newThread;
      if (conversation.getAiAssistantThreadId() == null) {
        newThread =
            service.notebookAssistantForNoteService.createThreadWithConversationContext(
                conversation);
        conversationService.setConversationAiAssistantThreadId(
            conversation, newThread.getThreadId());
      } else {
        newThread =
            service.notebookAssistantForNoteService.getThread(
                conversation.getAiAssistantThreadId());
      }
      return new MessageStage(conversation, conversationService, newThread, service);
    }
  }

  public static class MessageStage extends BaseStage {
    private final ChatAboutNoteService service;

    private MessageStage(
        Conversation conversation,
        ConversationService conversationService,
        AssistantThread thread,
        ChatAboutNoteService service) {
      super(conversation, conversationService, thread);
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

  public static class FinalStage extends BaseStage {
    private FinalStage(
        Conversation conversation,
        ConversationService conversationService,
        AssistantThread thread) {
      super(conversation, conversationService, thread);
    }

    public SseEmitter getReplyStream() {
      return thread
          .withAdditionalAdditionalInstructions(
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
