package com.odde.doughnut.services.ai.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.*;
import java.util.ArrayList;
import java.util.List;

public class OpenAIChatRequestBuilder {
  public final List<ChatMessage> messages = new ArrayList<>();
  public final List<ChatFunction> functions = new ArrayList<>();
  ChatCompletionRequest.ChatCompletionRequestBuilder builder = ChatCompletionRequest.builder();

  public OpenAIChatRequestBuilder model(String modelName) {
    builder.model(modelName);
    return this;
  }

  public OpenAIChatRequestBuilder maxTokens(int maxTokens) {
    builder.maxTokens(maxTokens);
    return this;
  }

  public List<ChatMessage> buildMessages() {
    return messages;
  }

  public ChatCompletionRequest build() {
    ChatCompletionRequest.ChatCompletionRequestBuilder requestBuilder =
        builder
            .messages(messages)
            //
            // an effort has been made to make the api call more responsive by using stream(true)
            // however, due to the library limitation, we cannot do it yet.
            // find more details here:
            //    https://github.com/TheoKanning/openai-java/issues/83
            .stream(false)
            .n(1);
    if (!functions.isEmpty()) {
      requestBuilder.functions(functions);
    }
    return requestBuilder.build();
  }

  public OpenAIChatRequestBuilder addSystemMessage(String message) {
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), message));
    return this;
  }

  public OpenAIChatRequestBuilder addUserMessage(String message) {
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), message));
    return this;
  }

  public void addFunctionCallMessage(Object arguments, String evaluateQuestion) {
    ChatMessage msg = new ChatMessage(ChatMessageRole.ASSISTANT.value(), null);
    msg.setFunctionCall(
        new ChatFunctionCall(evaluateQuestion, new ObjectMapper().valueToTree(arguments)));
    messages.add(msg);
  }
}
