package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.AssistantThread;
import com.odde.doughnut.services.ai.OpenAiAssistant;
import com.theokanning.openai.assistants.message.MessageRequest;
import java.util.ArrayList;
import java.util.List;

public class NotebookAssistantForNoteService {
  private final OpenAiAssistant assistantService;
  private final ObjectMapper objectMapper;
  final Note note;

  public NotebookAssistantForNoteService(
      OpenAiAssistant openAiAssistant, Note note, ObjectMapper objectMapper) {
    this.assistantService = openAiAssistant;
    this.note = note;
    this.objectMapper = objectMapper;
  }

  protected AssistantThread createThreadWithNoteInfo(List<MessageRequest> additionalMessages) {
    List<MessageRequest> messages = new ArrayList<>();
    messages.add(
        MessageRequest.builder().role("assistant").content(note.getNoteDescription()).build());
    if (!additionalMessages.isEmpty()) {
      messages.addAll(additionalMessages);
    }
    return assistantService.createThread(messages, note.getNotebookAssistantInstructions());
  }

  public AssistantThread getThread(String threadId) {
    return assistantService.getThread(threadId, note.getNotebookAssistantInstructions());
  }
}
