package com.odde.doughnut.services.ai;

public sealed interface OpenAiRunResult permits OpenAiRunRequiredAction, OpenAiRunCompleted {
  // This interface is just for type safety, no common methods needed
}
