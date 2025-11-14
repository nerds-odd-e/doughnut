package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ChatCompletionStreamTest {

  @Test
  void shouldCreateSseEmitterFromFlowable() {
    // Given an empty flowable (just testing construction)
    Flowable<ChatCompletionChunk> flowable = Flowable.empty();
    ChatCompletionStream stream = new ChatCompletionStream(flowable);

    // When getting SSE emitter
    SseEmitter emitter = stream.getSseEmitter(null);

    // Then emitter should be created
    assertNotNull(emitter);
  }

  @Test
  void shouldHandleEmptyStream() {
    // Given an empty stream
    Flowable<ChatCompletionChunk> flowable = Flowable.empty();
    ChatCompletionStream stream = new ChatCompletionStream(flowable);

    // When getting SSE emitter
    SseEmitter emitter = stream.getSseEmitter(null);

    // Then emitter should be created without error
    assertNotNull(emitter);
  }

  @Test
  void shouldAcceptContentConsumer() {
    // Given a stream
    Flowable<ChatCompletionChunk> flowable = Flowable.empty();
    ChatCompletionStream stream = new ChatCompletionStream(flowable);

    // When providing a content consumer
    final List<String> consumed = new ArrayList<>();
    SseEmitter emitter = stream.getSseEmitter(consumed::add);

    // Then should accept the consumer without error
    assertNotNull(emitter);
  }
}
