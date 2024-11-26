package com.odde.doughnut.services;

import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.AssistantThread;
import com.odde.doughnut.services.ai.OpenAiAssistant;
import com.odde.doughnut.services.commands.GetAiStreamHelper;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ChatAboutNoteService {
  protected final NotebookAssistantForNoteService1 notebookAssistantForNoteService1;
  protected final Note note;

  public ChatAboutNoteService(OpenAiAssistant openAiAssistant, Note note) {
    notebookAssistantForNoteService1 = new NotebookAssistantForNoteService1(openAiAssistant, note);
    this.note = note;
  }

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService) {

    AssistantThread thread;
    if (conversation.getAiAssistantThreadId() == null) {
      thread = notebookAssistantForNoteService1.createThreadWithNoteInfo(List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, thread.getThreadId());
    } else {
      thread = notebookAssistantForNoteService1.getThread(conversation.getAiAssistantThreadId());
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
