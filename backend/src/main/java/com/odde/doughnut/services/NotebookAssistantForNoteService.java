package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.AssistantService;
import com.odde.doughnut.services.commands.GetAiStreamCommand;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class NotebookAssistantForNoteService {
  private final AssistantService assistantService;

  public NotebookAssistantForNoteService(AssistantService assistantService) {
    this.assistantService = assistantService;
  }

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService, Note note) {
    return new GetAiStreamCommand(conversation, conversationService, note, assistantService)
        .execute();
  }

  public String suggestTopicTitle(Note note) {
    return assistantService.suggestTopicTitle(note);
  }
}
