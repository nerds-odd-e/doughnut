package com.odde.doughnut.services.ai;

import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class OpenAiRunStream {
  private final Flowable<AssistantSSE> runStream;

  public OpenAiRunStream(Flowable<AssistantSSE> runStream) {
    this.runStream = runStream;
  }

  public SseEmitter getSseEmitter(Consumer<Message> messageConsumer) {
    SseEmitter emitter = new SseEmitter();
    this.runStream.subscribe(
        sse -> {
          try {
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name(sse.getEvent().eventName).data(sse.getData());
            emitter.send(builder);

            // Handle thread.message.completed event
            if (Objects.equals(sse.getEvent().eventName, "thread.message.completed")) {
              Message message =
                  new com.odde.doughnut.configs.ObjectMapperConfig()
                      .objectMapper()
                      .readValue(sse.getData(), Message.class);
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
