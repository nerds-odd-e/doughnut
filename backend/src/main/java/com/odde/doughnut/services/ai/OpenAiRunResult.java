package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;

public sealed interface OpenAiRunResult permits OpenAiRunRequiredAction, OpenAiRunCompleted {
  default <T> T getAssumedToolCallArgument(Class<T> expectedType) throws JsonProcessingException {
    return switch (this) {
      case OpenAiRunRequiredAction action -> {
        T argument = expectedType.cast(action.getTheOnlyArgument());
        action.cancelRun();
        yield argument;
      }
      case OpenAiRunCompleted _ -> null;
    };
  }

  default <T> T getLastToolCallArgument(Class<T> expectedType) throws JsonProcessingException {
    return switch (this) {
      case OpenAiRunRequiredAction action -> {
        T argument = expectedType.cast(action.getLastArgument());
        action.cancelRun();
        yield argument;
      }
      case OpenAiRunCompleted _ -> null;
    };
  }
}
