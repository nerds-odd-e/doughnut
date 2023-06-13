package com.odde.doughnut.testability.builders;

import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionResult;
import java.util.ArrayList;
import java.util.List;

public class OpenAICompletionResultBuilder {
  private List<CompletionChoice> choices = new ArrayList<>();

  public OpenAICompletionResultBuilder choice(String text) {
    choices.add(
        new CompletionChoice() {
          {
            this.setText(text);
          }
        });
    return this;
  }

  public OpenAICompletionResultBuilder choiceReachingLengthLimit(String incompleteText) {
    choices.add(
        new CompletionChoice() {
          {
            this.setText(incompleteText);
            this.setFinish_reason("length");
          }
        });
    return this;
  }

  public CompletionResult please() {
    CompletionResult completionResult = new CompletionResult();
    completionResult.setChoices(choices);
    return completionResult;
  }
}
