package com.odde.doughnut.services.ai;

import static org.junit.jupiter.api.Assertions.*;

import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ConversationAiReplySseStreamTest {

  @Test
  void shouldCreateSseEmitterFromFlowable() {
    Flowable<String> flowable = Flowable.empty();
    ConversationAiReplySseStream stream = new ConversationAiReplySseStream(flowable);

    SseEmitter emitter = stream.getSseEmitter(null);

    assertNotNull(emitter);
  }

  @Test
  void shouldHandleEmptyStream() {
    Flowable<String> flowable = Flowable.empty();
    ConversationAiReplySseStream stream = new ConversationAiReplySseStream(flowable);

    SseEmitter emitter = stream.getSseEmitter(null);

    assertNotNull(emitter);
  }

  @Test
  void shouldAcceptContentConsumer() {
    Flowable<String> flowable = Flowable.empty();
    ConversationAiReplySseStream stream = new ConversationAiReplySseStream(flowable);

    final List<String> consumed = new ArrayList<>();
    SseEmitter emitter = stream.getSseEmitter(consumed::add);

    assertNotNull(emitter);
  }
}
