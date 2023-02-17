package com.odde.doughnut.entities.json;

import com.theokanning.openai.completion.CompletionChoice;
import lombok.Getter;

public final class AiSuggestion {
  @Getter private final String suggestion;
  @Getter private final String finishReason;

  public AiSuggestion(CompletionChoice choice) {
    this.suggestion = choice.getText();
    this.finishReason = choice.getFinish_reason();
  }
}
