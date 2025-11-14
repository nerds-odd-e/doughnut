package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import io.reactivex.Flowable;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ChatCompletionStream {
  private final Flowable<ChatCompletionChunk> chatStream;

  public ChatCompletionStream(Flowable<ChatCompletionChunk> chatStream) {
    this.chatStream = chatStream;
  }

  public SseEmitter getSseEmitter(Consumer<String> contentConsumer) {
    SseEmitter emitter = new SseEmitter();
    StringBuilder accumulatedContent = new StringBuilder();

    this.chatStream.subscribe(
        chunk -> {
          try {
            // Extract delta content from the chunk
            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
              var choice = chunk.getChoices().get(0);

              // Handle delta content
              String content = choice.getMessage().getContent();
              if (content != null && !content.isEmpty()) {
                accumulatedContent.append(content);

                // Emit the chunk as SSE event
                ObjectMapper mapper =
                    new com.odde.doughnut.configs.ObjectMapperConfig().objectMapper();
                String chunkJson = mapper.writeValueAsString(chunk);
                SseEmitter.SseEventBuilder builder =
                    SseEmitter.event().name("chat.completion.chunk").data(chunkJson);
                emitter.send(builder);
              }

              // Check for completion
              if (choice.getFinishReason() != null && !choice.getFinishReason().isEmpty()) {
                // Call the consumer with accumulated content when done
                if (contentConsumer != null && accumulatedContent.length() > 0) {
                  contentConsumer.accept(accumulatedContent.toString());
                }

                // Send done event and complete
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
          // On complete without finish reason, still call consumer
          if (contentConsumer != null && accumulatedContent.length() > 0) {
            contentConsumer.accept(accumulatedContent.toString());
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
