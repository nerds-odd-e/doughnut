package com.odde.doughnut.services.ai;

import com.openai.models.ReasoningEffort;
import java.util.Locale;

public final class OpenAiModelCapabilities {

  private OpenAiModelCapabilities() {}

  public static boolean supportsReasoningEffort(String modelName) {
    if (modelName == null || modelName.isBlank()) {
      return false;
    }
    String normalized = modelName.toLowerCase(Locale.ROOT);
    return normalized.startsWith("o1")
        || normalized.startsWith("o3")
        || normalized.startsWith("o4")
        || normalized.startsWith("gpt-5");
  }

  public static ReasoningEffort questionGenerationReasoningEffort(String modelName, boolean batch) {
    if (!supportsReasoningEffort(modelName)) {
      return ReasoningEffort.NONE;
    }
    return batch ? ReasoningEffort.HIGH : ReasoningEffort.MEDIUM;
  }

  public static long questionGenerationMaxOutputTokens(
      ReasoningEffort reasoningEffort, boolean batch) {
    if (reasoningEffort == ReasoningEffort.NONE) {
      return 1000L;
    }
    return batch ? 4000L : 2000L;
  }
}
