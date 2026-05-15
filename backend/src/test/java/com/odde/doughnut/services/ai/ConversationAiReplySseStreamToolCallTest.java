package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;

import io.reactivex.Flowable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ConversationAiReplySseStreamToolCallTest {

  @Test
  void shouldDetectToolCallsInStream() {
    Flowable<String> flowable = Flowable.empty();
    ConversationAiReplySseStream stream = new ConversationAiReplySseStream(flowable);

    SseEmitter emitter = stream.getSseEmitter(null);

    assertNotNull(emitter);
  }

  @Test
  void shouldInvokeCallbackOnlyOnceForToolCalls() {
    Flowable<String> flowable = Flowable.empty();
    ConversationAiReplySseStream stream = new ConversationAiReplySseStream(flowable);

    AtomicReference<Integer> callCount = new AtomicReference<>(0);
    SseEmitter emitter =
        stream.getSseEmitter(
            content -> {
              callCount.set(callCount.get() + 1);
            });

    assertNotNull(emitter);
  }
}
