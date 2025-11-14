package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import io.reactivex.Flowable;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ChatCompletionStream {
  private final Flowable<String> chatStream; // Raw JSON strings instead of chunks

  public ChatCompletionStream(Flowable<String> chatStream) {
    this.chatStream = chatStream;
  }

  public SseEmitter getSseEmitter(Consumer<String> contentConsumer) {
    SseEmitter emitter = new SseEmitter();
    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    StringBuilder accumulatedContent = new StringBuilder();
    final boolean[] consumerCalled = {false};

    this.chatStream.subscribe(
        rawJson -> {
          try {
            // Emit raw JSON directly to preserve delta field
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name("chat.completion.chunk").data(rawJson);
            emitter.send(builder);

            // Parse raw JSON to extract delta content for callback
            JsonNode chunkNode = mapper.readTree(rawJson);
            JsonNode choicesNode = chunkNode.get("choices");

            if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
              JsonNode choiceNode = choicesNode.get(0);

              // Parse delta from raw JSON (streaming chunks use delta)
              JsonNode deltaNode = choiceNode.get("delta");
              if (deltaNode != null && !deltaNode.isNull()) {
                JsonNode contentNode = deltaNode.get("content");
                if (contentNode != null && contentNode.isTextual()) {
                  String deltaContent = contentNode.asText();
                  accumulatedContent.append(deltaContent);
                }

                // Tool calls in delta are forwarded as raw JSON to frontend
                // Frontend handles accumulation of fragmented tool call arguments
              }

              // Also check message field (for final chunks)
              JsonNode messageNode = choiceNode.get("message");
              if (messageNode != null) {
                JsonNode messageContentNode = messageNode.get("content");
                if (messageContentNode != null && messageContentNode.isTextual()) {
                  accumulatedContent.append(messageContentNode.asText());
                }
              }

              // Check finish_reason for completion
              JsonNode finishReasonNode = choiceNode.get("finish_reason");
              if (finishReasonNode != null && finishReasonNode.isTextual() && !consumerCalled[0]) {
                String finishReason = finishReasonNode.asText();
                // Only call consumer for text responses, not tool calls
                if (!"tool_calls".equals(finishReason) && accumulatedContent.length() > 0) {
                  contentConsumer.accept(accumulatedContent.toString());
                }
                consumerCalled[0] = true;

                // Send done and complete
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
          // Send error event through SSE before completing with error
          // This allows the frontend to handle the error properly
          try {
            if (error instanceof OpenAiUnauthorizedException) {
              // Send error event that frontend can catch
              SseEmitter.SseEventBuilder errorBuilder =
                  SseEmitter.event().name("error").data("Bad Request");
              emitter.send(errorBuilder);
              // Complete normally after sending error event so frontend can process it
              emitter.complete();
            } else {
              emitter.completeWithError(error);
            }
          } catch (Exception e) {
            // If sending fails, just complete with error
            emitter.completeWithError(error);
          }
        },
        () -> {
          // On stream complete, invoke callback if not already called
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
