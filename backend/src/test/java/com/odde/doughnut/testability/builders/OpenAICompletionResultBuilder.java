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
            this.setMessage(new AssistantMessage(text));
          }
        });
    return this;
  }

  public OpenAICompletionResultBuilder choiceReachingLengthLimit(String incompleteText) {
    choices.add(
        new ChatCompletionChoice() {
          {
            this.setMessage(new AssistantMessage(incompleteText));
            this.setFinishReason("length");
          }
        });
    return this;
  }

  public OpenAICompletionResultBuilder toolCall(String name, JsonNode arguments) {
    AssistantMessage message = new AssistantMessage("");
    ChatToolCall toolCall = new ChatToolCall();
    ChatFunctionCall function = new ChatFunctionCall(name, arguments);
    toolCall.setFunction(function);
    toolCall.setFunction(function);
    message.setToolCalls(List.of(toolCall));
    choices.add(
        new ChatCompletionChoice() {
          {
            this.setMessage(message);
            this.setFinishReason("tool_calls");
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
