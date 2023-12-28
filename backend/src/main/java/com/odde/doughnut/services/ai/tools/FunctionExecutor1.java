package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.*;

public class FunctionExecutor1 {
  private final ChatFunction function;
  private ObjectMapper MAPPER = new ObjectMapper();

  public FunctionExecutor1(ChatFunction function) {
    this.function = function;
  }

  public ChatMessage executeAndConvertToMessage(ChatFunctionCall call) {
    return new ChatMessage(
        ChatMessageRole.FUNCTION.value(),
        executeAndConvertToJson(call).toPrettyString(),
        call.getName());
  }

  public JsonNode executeAndConvertToJson(ChatFunctionCall call) {
    try {
      Object execution = execute(call);
      if (execution instanceof TextNode) {
        JsonNode objectNode = MAPPER.readTree(((TextNode) execution).asText());
        if (objectNode.isMissingNode()) return (JsonNode) execution;
        return objectNode;
      }
      if (execution instanceof ObjectNode) {
        return (JsonNode) execution;
      }
      if (execution instanceof String) {
        JsonNode objectNode = MAPPER.readTree((String) execution);
        if (objectNode.isMissingNode()) throw new RuntimeException("Parsing exception");
        return objectNode;
      }
      return MAPPER.readValue(MAPPER.writeValueAsString(execution), JsonNode.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T execute(ChatFunctionCall call) {
    Object obj;
    try {
      JsonNode arguments = call.getArguments();
      obj =
          MAPPER.readValue(
              arguments instanceof TextNode ? arguments.asText() : arguments.toPrettyString(),
              function.getParametersClass());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return (T) function.getExecutor().apply(obj);
  }
}
