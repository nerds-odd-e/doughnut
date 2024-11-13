package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.AssistantService;
import com.odde.doughnut.services.commands.GetAiStreamCommand;
import com.theokanning.openai.assistants.message.MessageRequest;
import java.util.ArrayList;
import java.util.List;
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

    String threadId = conversation.getAiAssistantThreadId();
    if (threadId == null) {
      threadId = createThread(List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, threadId);
    }

    return new GetAiStreamCommand(conversation, conversationService, note, assistantService)
        .execute(threadId);
  }

  private String createThread(List<MessageRequest> additionalMessages) {
    List<MessageRequest> messages =
        new ArrayList<>(
            List.of(
                MessageRequest.builder()
                    .role("assistant")
                    .content(note.getNoteDescription())
                    .build()));
    if (additionalMessages != null && !additionalMessages.isEmpty()) {
      messages.addAll(additionalMessages);
    }
    return assistantService.createThread(messages);
  }

  public String suggestTopicTitle() {
    String threadId =
        createThread(
            List.of(
                MessageRequest.builder()
                    .role("user")
                    .content("Please suggest a better topic title for the note.")
                    .build()));
    AiAssistantResponse threadResponse = assistantService.createRunAndGetThreadResponse(threadId);
    assistantService.getAssistantRunService(threadId, threadResponse.getRunId()).cancelRun();
    return threadResponse
        .getToolCalls()
        .getFirst()
        .getFunction()
        .getArguments()
        .get("newTopic")
        .asText();
  }
}
