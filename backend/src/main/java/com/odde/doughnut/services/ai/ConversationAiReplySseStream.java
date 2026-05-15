package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import io.reactivex.Flowable;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ConversationAiReplySseStream {
  private final Flowable<String> chunkJsonStream;

  public ConversationAiReplySseStream(Flowable<String> chunkJsonStream) {
    this.chunkJsonStream = chunkJsonStream;
  }

  public SseEmitter getSseEmitter(Consumer<String> contentConsumer) {
    SseEmitter emitter = new SseEmitter();
    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    StringBuilder accumulatedContent = new StringBuilder();
    final boolean[] consumerCalled = {false};

    this.chunkJsonStream.subscribe(
        rawJson -> {
          try {
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name("chat.completion.chunk").data(rawJson);
            emitter.send(builder);

            JsonNode chunkNode = mapper.readTree(rawJson);
            JsonNode choicesNode = chunkNode.get("choices");

            if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
              JsonNode choiceNode = choicesNode.get(0);

              JsonNode deltaNode = choiceNode.get("delta");
              if (deltaNode != null && !deltaNode.isNull()) {
                JsonNode contentNode = deltaNode.get("content");
                if (contentNode != null && contentNode.isTextual()) {
                  String deltaContent = contentNode.asText();
                  accumulatedContent.append(deltaContent);
                }
              }

              JsonNode finishReasonNode = choiceNode.get("finish_reason");
              if (finishReasonNode != null && finishReasonNode.isTextual() && !consumerCalled[0]) {
                String finishReason = finishReasonNode.asText();
                if (!"tool_calls".equals(finishReason) && accumulatedContent.length() > 0) {
                  contentConsumer.accept(accumulatedContent.toString());
                }
                consumerCalled[0] = true;

                SseEmitter.SseEventBuilder doneBuilder =
                    SseEmitter.event().name("done").data("[DONE]");
                emitter.send(doneBuilder);
                emitter.complete();
              }
            }
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        },
        error -> {
          try {
            if (error instanceof OpenAiUnauthorizedException) {
              SseEmitter.SseEventBuilder errorBuilder =
                  SseEmitter.event().name("error").data("Bad Request");
              emitter.send(errorBuilder);
              emitter.complete();
            } else {
              emitter.completeWithError(error);
            }
          } catch (Exception e) {
            emitter.completeWithError(error);
          }
        },
        () -> {
          if (contentConsumer != null && accumulatedContent.length() > 0 && !consumerCalled[0]) {
            contentConsumer.accept(accumulatedContent.toString());
            consumerCalled[0] = true;
          }
          try {
            emitter.complete();
          } catch (Exception ignored) {
            // Already completed
          }
        });

    return emitter;
  }
}
