package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;

public sealed interface OpenAiRunResult permits OpenAiRunRequiredAction, OpenAiRunCompleted {
  // Common method to handle OpenAiRunResult processing
  default <T> T getAssumedToolCallArgument(Class<T> expectedType) throws JsonProcessingException {
    return switch (this) {
      case OpenAiRunRequiredAction action -> {
        T argument = expectedType.cast(action.getFirstArgument());
        action.cancelRun();
        yield argument;
      }
      case OpenAiRunCompleted _ -> null;
    };
  }
  // This interface is just for type safety, no common methods needed
}
