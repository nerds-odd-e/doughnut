package com.odde.doughnut.services.ai.tools;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.*;

public class AiToolList {
  final Map<String, ChatFunction> functions = new HashMap<>();
  private String messageBody;

  public AiToolList(String message, List<ChatFunction> functions) {
    this.messageBody = message;
    functions.forEach(f -> this.functions.put(f.getName(), f));
  }

  public Collection<ChatFunction> getFunctions() {
    return new ArrayList<>(functions.values());
  }

  public String getUserRequestMessage() {
    return messageBody;
  }

  public String getFirstFunctionName() {
    return functions.keySet().iterator().next();
  }

  public static ChatMessage functionCall(String functionName, Object arguments) {
    ChatMessage functionCallMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value());
    functionCallMessage.setFunctionCall(
        new ChatFunctionCall(functionName, defaultObjectMapper().valueToTree(arguments)));
    return functionCallMessage;
  }

  public static ChatMessage functionCallResponse(String functionName, Object resp) {
    JsonNode jsonNode = defaultObjectMapper().convertValue(resp, JsonNode.class);
    return new ChatMessage(
        ChatMessageRole.FUNCTION.value(), jsonNode.toPrettyString(), functionName);
  }
}
