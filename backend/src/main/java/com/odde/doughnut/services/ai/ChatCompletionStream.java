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
    final boolean[] consumerCalled = {false}; // Track if consumer already called

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

                // Emit delta event compatible with frontend expectations
                ObjectMapper mapper =
                    new com.odde.doughnut.configs.ObjectMapperConfig().objectMapper();
                String deltaJson =
                    String.format(
                        "{\"delta\": {\"content\": [{\"type\": \"text\", \"text\": {\"value\": \"%s\"}}]}}",
                        content.replace("\"", "\\\"").replace("\n", "\\n"));
                SseEmitter.SseEventBuilder deltaBuilder =
                    SseEmitter.event().name("thread.message.delta").data(deltaJson);
                emitter.send(deltaBuilder);
              }

              // Check for completion
              if (choice.getFinishReason() != null && !choice.getFinishReason().isEmpty()) {
                // Call the consumer with accumulated content when done
                if (contentConsumer != null
                    && accumulatedContent.length() > 0
                    && !consumerCalled[0]) {
                  contentConsumer.accept(accumulatedContent.toString());
                  consumerCalled[0] = true;
                }

                // Send message completed event
                String completedJson =
                    String.format(
                        "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":{\"value\":\"%s\"}}]}",
                        accumulatedContent.toString().replace("\"", "\\\"").replace("\n", "\\n"));
                SseEmitter.SseEventBuilder completedBuilder =
                    SseEmitter.event().name("thread.message.completed").data(completedJson);
                emitter.send(completedBuilder);

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
          // On complete without finish reason, still call consumer (only if not already called)
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
