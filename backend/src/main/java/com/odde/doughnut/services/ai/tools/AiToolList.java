package com.odde.doughnut.services.ai.tools;

import static com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder.askClarificationQuestion;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.*;

public class AiToolList {
  final Map<String, ChatFunction> FUNCTIONS = new HashMap<>();

  public AiToolList(List<ChatFunction> functions) {
    functions.forEach(f -> this.FUNCTIONS.put(f.getName(), f));
  }

  public Collection<ChatFunction> getFunctions() {
    return new ArrayList<>(FUNCTIONS.values());
  }

  public List<ChatMessage> functionReturningMessages(Object arguments, Object resp) {
    ChatMessage functionCallMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value());
    functionCallMessage.setFunctionCall(
        new ChatFunctionCall(
            askClarificationQuestion, defaultObjectMapper().valueToTree(arguments)));
    ChatMessage functionCallResponse =
        functionCallResponse(functionCallMessage.getFunctionCall().getName(), resp);
    return List.of(functionCallMessage, functionCallResponse);
  }

  private ChatMessage functionCallResponse(String functionName, Object resp) {
    JsonNode jsonNode = defaultObjectMapper().convertValue(resp, JsonNode.class);
    return new ChatMessage(
        ChatMessageRole.FUNCTION.value(), jsonNode.toPrettyString(), functionName);
  }
}
