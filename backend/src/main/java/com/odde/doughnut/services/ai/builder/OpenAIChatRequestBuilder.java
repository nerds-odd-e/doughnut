package com.odde.doughnut.services.ai.builder;

import com.odde.doughnut.services.ai.OpenAIChatAboutNoteRequestBuilder;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

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

  public OpenAIChatRequestBuilder addMessage(ChatMessageRole role, String userMessage) {
    messages.add(new ChatMessage(role.value(), userMessage));
    return this;
  }
}
