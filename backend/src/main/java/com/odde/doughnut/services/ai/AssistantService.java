package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class AssistantService {
  private final OpenAiApiHandler openAiApiHandler;
  private final String assistantId;

  public AssistantService(OpenAiApiHandler openAiApiHandler, String assistantId) {
    this.openAiApiHandler = openAiApiHandler;
    this.assistantId = assistantId;
  }

  public Flowable<AssistantSSE> getRunStream(String threadId) {
    return openAiApiHandler.createRunStream(threadId, assistantId);
  }

  public void createUserMessage(String prompt, AssistantThread thread) {
    MessageRequest messageRequest = MessageRequest.builder().role("user").content(prompt).build();
    openAiApiHandler.createMessage(thread.threadId, messageRequest);
  }

  public void createAssistantMessage(String msg, AssistantThread thread) {
    MessageRequest messageRequest = MessageRequest.builder().role("assistant").content(msg).build();
    openAiApiHandler.createMessage(thread.threadId, messageRequest);
  }

  public AssistantThread getThread(String threadId) {
    return new AssistantThread(assistantId, threadId, openAiApiHandler);
  }

  public AssistantThread createThread(List<MessageRequest> additionalMessages) {
    List<MessageRequest> messages =
        new ArrayList<>(
            List.of(
                MessageRequest.builder()
                    .role("assistant")
                    .content("Please only call function to update content when user asks to.")
                    .build()));

    if (additionalMessages != null && !additionalMessages.isEmpty()) {
      messages.addAll(additionalMessages);
    }
    ThreadRequest threadRequest = ThreadRequest.builder().messages(messages).build();
    String threadId = openAiApiHandler.createThread(threadRequest).getId();
    return getThread(threadId);
  }

  public SseEmitter getRunStreamAsSSE(Consumer<Message> messageConsumer, AssistantThread thread) {
    Flowable<AssistantSSE> runStream = getRunStream(thread.threadId);
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
              if (messageConsumer != null) {
                messageConsumer.accept(message);
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
}
