package com.odde.donut.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.exceptions.OpenAiUnauthorizedException;
import io.reactivex.Flowable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class ConversationAiReplySseStream {
  private final Flowable<String> chunkJsonStream;

  public ConversationAiReplySseStream(Flowable<String> chunkJsonStream) {
    this.chunkJsonStream = chunkJsonStream;
  }

  public record ToolCall(String id, String name, String arguments) {}

  public record Reply(String content, List<ToolCall> toolCalls) {}

  private static class AccumulatedToolCall {
    String id;
    String name;
    StringBuilder arguments = new StringBuilder();
  }

  public SseEmitter getSseEmitter(Consumer<Reply> replyConsumer) {
    SseEmitter emitter = new SseEmitter();
    ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
    StringBuilder content = new StringBuilder();
    Map<Integer, AccumulatedToolCall> tools = new LinkedHashMap<>();

    this.chunkJsonStream.subscribe(
        rawJson -> {
          try {
            emitter.send(SseEmitter.event().name("chat.completion.chunk").data(rawJson));
            JsonNode delta = mapper.readTree(rawJson).path("choices").path(0).path("delta");
            if (delta.path("content").isTextual()) {
              content.append(delta.path("content").asText());
            }
            for (JsonNode tool : delta.path("tool_calls")) {
              AccumulatedToolCall accumulated =
                  tools.computeIfAbsent(
                      tool.path("index").asInt(), ignored -> new AccumulatedToolCall());
              if (tool.has("id")) accumulated.id = tool.path("id").asText();
              JsonNode function = tool.path("function");
              if (function.has("name")) accumulated.name = function.path("name").asText();
              accumulated.arguments.append(function.path("arguments").asText(""));
            }
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        },
        error -> {
          try {
            if (error instanceof OpenAiUnauthorizedException) {
              emitter.send(SseEmitter.event().name("error").data("Bad Request"));
              emitter.complete();
            } else {
              emitter.completeWithError(error);
            }
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        },
        () -> {
          try {
            replyConsumer.accept(
                new Reply(
                    content.toString(),
                    tools.values().stream()
                        .map(tool -> new ToolCall(tool.id, tool.name, tool.arguments.toString()))
                        .toList()));
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        });
    return emitter;
  }
}
