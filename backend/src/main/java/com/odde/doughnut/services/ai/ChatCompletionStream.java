package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatToolCall;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ChatCompletionStream {
  private final Flowable<ChatCompletionChunk> chatStream;

  public ChatCompletionStream(Flowable<ChatCompletionChunk> chatStream) {
    this.chatStream = chatStream;
  }

  public SseEmitter getSseEmitter(Consumer<String> contentConsumer) {
    SseEmitter emitter = new SseEmitter();
    ObjectMapper mapper = new com.odde.doughnut.configs.ObjectMapperConfig().objectMapper();
    StringBuilder accumulatedContent = new StringBuilder();
    List<ChatToolCall> accumulatedToolCalls = new ArrayList<>();
    final boolean[] consumerCalled = {false};

    this.chatStream.subscribe(
        chunk -> {
          try {
            // Emit native chat completion chunk
            String chunkJson = mapper.writeValueAsString(chunk);
            SseEmitter.SseEventBuilder builder =
                SseEmitter.event().name("chat.completion.chunk").data(chunkJson);
            emitter.send(builder);

            // Accumulate for callback
            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
              var choice = chunk.getChoices().get(0);
              if (choice.getMessage() instanceof AssistantMessage assistantMessage) {
                String content = assistantMessage.getContent();
                if (content != null) {
                  accumulatedContent.append(content);
                }
                if (assistantMessage.getToolCalls() != null) {
                  accumulatedToolCalls.addAll(assistantMessage.getToolCalls());
                }
              }

              // On completion, invoke callback
              if (choice.getFinishReason() != null
                  && !choice.getFinishReason().isEmpty()
                  && !consumerCalled[0]) {
                // Only call consumer for text responses, not tool calls
                if (!"tool_calls".equals(choice.getFinishReason())
                    && accumulatedContent.length() > 0) {
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
        error -> emitter.completeWithError(error),
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
