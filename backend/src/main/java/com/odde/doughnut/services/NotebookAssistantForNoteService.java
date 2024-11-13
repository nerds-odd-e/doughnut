package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.AssistantService;
import com.odde.doughnut.services.commands.GetAiStreamCommand;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class NotebookAssistantForNoteService {
  private final AssistantService assistantService;
  private final Note note;

  public NotebookAssistantForNoteService(AssistantService assistantService, Note note) {
    this.assistantService = assistantService;
    this.note = note;
  }

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService) {
    return new GetAiStreamCommand(conversation, conversationService, note, assistantService)
        .execute();
  }

  public String suggestTopicTitle() {
    return assistantService.suggestTopicTitle(note);
  }
}
