package com.odde.doughnut.testability.builders;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
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

  public ChatCompletionResult please() {
    ChatCompletionResult chatCompletionResult = new ChatCompletionResult();
    chatCompletionResult.setChoices(choices);
    return chatCompletionResult;
  }
}
