package com.odde.doughnut.testability.builders;

import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.*;
import java.util.ArrayList;
import java.util.List;

public class OpenAICompletionResultBuilder {
  private final List<ChatCompletionChoice> choices = new ArrayList<>();

  public OpenAICompletionResultBuilder choice(String text) {
    choices.add(
        new ChatCompletionChoice() {
          {
            this.setMessage(new ChatMessage(ChatMessageRole.USER.value(), text));
          }
        });
    return this;
  }

  public OpenAICompletionResultBuilder choiceReachingLengthLimit(String incompleteText) {
    choices.add(
        new ChatCompletionChoice() {
          {
            this.setMessage(new ChatMessage(ChatMessageRole.USER.value(), incompleteText));
            this.setFinishReason("length");
          }
        });
    return this;
  }

  public OpenAICompletionResultBuilder functionCall(String name, JsonNode arguments) {
    ChatMessage message = new ChatMessage(ChatMessageRole.FUNCTION.value(), "");
    message.setFunctionCall(new ChatFunctionCall(name, arguments));
    choices.add(
        new ChatCompletionChoice() {
          {
            this.setMessage(message);
            this.setFinishReason("function call");
          }
        });
    return this;
  }

  public ChatCompletionResult please() {
    ChatCompletionResult chatCompletionResult = new ChatCompletionResult();
    chatCompletionResult.setChoices(choices);
    return chatCompletionResult;
  }
}
