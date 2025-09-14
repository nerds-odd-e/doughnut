package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;

public sealed interface OpenAiRunResult permits OpenAiRunRequiredAction, OpenAiRunCompleted {
  default <T> T getAssumedToolCallArgument(Class<T> expectedType) throws JsonProcessingException {
    return switch (this) {
      case OpenAiRunRequiredAction action -> expectedType.cast(action.getTheOnlyArgument());
      case OpenAiRunCompleted completed -> null;
    };
  }

  default <T> T getLastToolCallArgument(Class<T> expectedType) throws JsonProcessingException {
    return switch (this) {
      case OpenAiRunRequiredAction action -> expectedType.cast(action.getLastArgument());
      case OpenAiRunCompleted completed -> null;
    };
  }
}
