package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
public class ChatAboutNoteService {
  private final String threadId;
  private final AssistantService assistantService;
  private final ModelFactoryService modelFactoryService;

  public SseEmitter getAIReplySSE() {
    Flowable<AssistantSSE> runStream = assistantService.getRunStream(threadId);
    SseEmitter emitter = new SseEmitter();
    runStream.subscribe(
        sse -> {
          try {
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name(sse.getEvent().eventName).data(sse.getData());
            emitter.send(builder);

            // Handle thread.message.completed event
            if (Objects.equals(sse.getEvent().eventName, "thread.message.completed")) {
              Message message = new ObjectMapper().readValue(sse.getData(), Message.class);
              if (messageCompletedCallback != null) {
                messageCompletedCallback.accept(message);
              }
            }

            if (Objects.equals(sse.getEvent().eventName, "done")) {
              emitter.complete();
            }
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        });
    return emitter;
  }

  public void createUserMessage(String userMessage) {
    assistantService.createUserMessage(userMessage, threadId);
  }

  private Consumer<Message> messageCompletedCallback;

  public void onMessageCompleted(Consumer<Message> callback) {
    this.messageCompletedCallback = callback;
  }

  public void sendNoteUpdateMessageIfNeeded(Note note, Conversation conversation) {
    if (conversation.getLastAiAssistantThreadSync() != null
        && note.getUpdatedAt().after(conversation.getLastAiAssistantThreadSync())) {
      assistantService.createAssistantMessage(
          "The note content has been update:\n\n%s".formatted(note.getNoteDescription()), threadId);
    }
  }
}
