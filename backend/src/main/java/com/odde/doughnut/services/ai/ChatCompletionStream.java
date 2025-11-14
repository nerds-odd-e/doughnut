package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    StringBuilder accumulatedContent = new StringBuilder();
    List<ChatToolCall> accumulatedToolCalls = new ArrayList<>();
    final boolean[] consumerCalled = {false}; // Track if consumer already called
    final boolean[] messageCreatedSent = {false}; // Track if initial event sent

    this.chatStream.subscribe(
        chunk -> {
          try {
            // Send thread.message.created event on first chunk
            if (!messageCreatedSent[0]) {
              String createdJson =
                  "{\"id\":\"msg-synthetic\",\"thread_id\":\"thread-synthetic\",\"run_id\":\"run-synthetic\",\"role\":\"assistant\",\"content\":[]}";
              SseEmitter.SseEventBuilder createdBuilder =
                  SseEmitter.event().name("thread.message.created").data(createdJson);
              emitter.send(createdBuilder);
              messageCreatedSent[0] = true;
            }

            // Extract delta content from the chunk
            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
              var choice = chunk.getChoices().get(0);

              // Accumulate message data
              if (choice.getMessage() instanceof AssistantMessage assistantMessage) {
                // Accumulate text content
                String content = assistantMessage.getContent();
                if (content != null && !content.isEmpty()) {
                  accumulatedContent.append(content);

                  // Emit delta event
                  String deltaJson =
                      String.format(
                          "{\"delta\": {\"content\": [{\"type\": \"text\", \"text\": {\"value\": \"%s\"}}]}}",
                          escapeJson(content));
                  SseEmitter.SseEventBuilder deltaBuilder =
                      SseEmitter.event().name("thread.message.delta").data(deltaJson);
                  emitter.send(deltaBuilder);
                }

                // Accumulate tool calls
                if (assistantMessage.getToolCalls() != null
                    && !assistantMessage.getToolCalls().isEmpty()) {
                  accumulatedToolCalls.addAll(assistantMessage.getToolCalls());
                }
              }

              // Check for completion
              if (choice.getFinishReason() != null && !choice.getFinishReason().isEmpty()) {
                handleCompletion(
                    emitter,
                    choice.getFinishReason(),
                    accumulatedContent.toString(),
                    accumulatedToolCalls,
                    contentConsumer,
                    consumerCalled);
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

  private void handleCompletion(
      SseEmitter emitter,
      String finishReason,
      String content,
      List<ChatToolCall> toolCalls,
      Consumer<String> contentConsumer,
      boolean[] consumerCalled) {
    try {
      if ("tool_calls".equals(finishReason) && !toolCalls.isEmpty()) {
        // Emit tool call event (thread.run.requires_action for frontend compatibility)
        emitToolCallEvent(emitter, toolCalls);
      } else {
        // Regular message completion
        if (contentConsumer != null && content.length() > 0 && !consumerCalled[0]) {
          contentConsumer.accept(content);
          consumerCalled[0] = true;
        }

        // Send message completed event
        String completedJson =
            String.format(
                "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":{\"value\":\"%s\"}}]}",
                escapeJson(content));
        SseEmitter.SseEventBuilder completedBuilder =
            SseEmitter.event().name("thread.message.completed").data(completedJson);
        emitter.send(completedBuilder);
      }

      // Send done event and complete
      SseEmitter.SseEventBuilder doneBuilder = SseEmitter.event().name("done").data("[DONE]");
      emitter.send(doneBuilder);
      emitter.complete();
    } catch (Exception e) {
      emitter.completeWithError(e);
    }
  }

  private void emitToolCallEvent(SseEmitter emitter, List<ChatToolCall> toolCalls)
      throws JsonProcessingException, java.io.IOException {
    ObjectMapper mapper = new com.odde.doughnut.configs.ObjectMapperConfig().objectMapper();

    // Format tool calls in Assistant API format for frontend compatibility
    StringBuilder toolCallsJson = new StringBuilder("[");
    for (int i = 0; i < toolCalls.size(); i++) {
      ChatToolCall toolCall = toolCalls.get(i);
      if (i > 0) toolCallsJson.append(",");

      // Get arguments as JSON string
      Object argumentsObj = toolCall.getFunction().getArguments();
      String argumentsJsonString;
      if (argumentsObj instanceof String) {
        argumentsJsonString = (String) argumentsObj;
      } else {
        argumentsJsonString = mapper.writeValueAsString(argumentsObj);
      }

      // Build tool call JSON manually to ensure proper string escaping
      // arguments should be a STRING containing JSON (will be parsed by frontend)
      toolCallsJson
          .append("{\"id\":\"")
          .append(toolCall.getId() != null ? toolCall.getId() : "tool-call-" + i)
          .append("\",\"type\":\"function\",\"function\":{\"name\":\"")
          .append(toolCall.getFunction().getName())
          .append("\",\"arguments\":")
          .append(mapper.writeValueAsString(argumentsJsonString)) // Serialize string as JSON string
          .append("}}");
    }
    toolCallsJson.append("]");

    // Emit thread.run.requires_action event
    String requiresActionJson =
        String.format(
            "{\"id\":\"run-synthetic\",\"thread_id\":\"thread-synthetic\",\"status\":\"requires_action\",\"required_action\":{\"type\":\"submit_tool_outputs\",\"submit_tool_outputs\":{\"tool_calls\":%s}}}",
            toolCallsJson);
    SseEmitter.SseEventBuilder requiresActionBuilder =
        SseEmitter.event().name("thread.run.requires_action").data(requiresActionJson);
    emitter.send(requiresActionBuilder);
  }

  private String escapeJson(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }
}
