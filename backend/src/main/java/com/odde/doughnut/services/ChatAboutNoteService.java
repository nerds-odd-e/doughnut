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

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService) {

    AssistantThread thread;
    if (conversation.getAiAssistantThreadId() == null) {
      thread = notebookAssistantForNoteService.createThreadWithNoteInfo(List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, thread.getThreadId());
    } else {
      thread = notebookAssistantForNoteService.getThread(conversation.getAiAssistantThreadId());
    }

    Timestamp lastAiAssistantThreadSync = conversation.getLastAiAssistantThreadSync();
    if (lastAiAssistantThreadSync != null && note.getUpdatedAt().after(lastAiAssistantThreadSync)) {
      thread.createAssistantMessage(
          "The note content has been update:\n\n%s".formatted(note.getNoteDescription()));
    }
    List<ConversationMessage> unseen = conversation.getUnseenMessagesByAssistant();
    if (!unseen.isEmpty()) {
      thread.createUserMessage(GetAiStreamHelper.formatUnsentMessages(unseen));
    }
    conversationService.updateLastAiAssistantThreadSync(conversation);

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
