package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.openai.models.ReasoningEffort;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenAiModelCapabilitiesTest {

  @Nested
  class SupportsReasoningEffort {
    @Test
    void returnsTrueForReasoningModelPrefixes() {
      assertThat(OpenAiModelCapabilities.supportsReasoningEffort("o3-mini"), is(true));
      assertThat(OpenAiModelCapabilities.supportsReasoningEffort("o4-mini"), is(true));
      assertThat(OpenAiModelCapabilities.supportsReasoningEffort("gpt-5"), is(true));
      assertThat(OpenAiModelCapabilities.supportsReasoningEffort("GPT-5-preview"), is(true));
    }

    @Test
    void returnsFalseForNonReasoningModels() {
      assertThat(OpenAiModelCapabilities.supportsReasoningEffort("gpt-4.1-mini"), is(false));
      assertThat(OpenAiModelCapabilities.supportsReasoningEffort("gpt-4o"), is(false));
      assertThat(OpenAiModelCapabilities.supportsReasoningEffort(""), is(false));
      assertThat(OpenAiModelCapabilities.supportsReasoningEffort(null), is(false));
    }
  }

  @Nested
  class QuestionGenerationReasoningEffort {
    @Test
    void usesMediumForSyncAndHighForBatchWhenSupported() {
      assertThat(
          OpenAiModelCapabilities.questionGenerationReasoningEffort("o3-mini", false),
          is(ReasoningEffort.MEDIUM));
      assertThat(
          OpenAiModelCapabilities.questionGenerationReasoningEffort("o3-mini", true),
          is(ReasoningEffort.HIGH));
    }

    @Test
    void usesNoneWhenModelDoesNotSupportReasoning() {
      assertThat(
          OpenAiModelCapabilities.questionGenerationReasoningEffort("gpt-4.1-mini", false),
          is(ReasoningEffort.NONE));
      assertThat(
          OpenAiModelCapabilities.questionGenerationReasoningEffort("gpt-4.1-mini", true),
          is(ReasoningEffort.NONE));
    }
  }

  @Nested
  class QuestionGenerationMaxOutputTokens {
    @Test
    void usesDefaultBudgetWithoutReasoning() {
      assertThat(
          OpenAiModelCapabilities.questionGenerationMaxOutputTokens(ReasoningEffort.NONE, false),
          is(1000L));
      assertThat(
          OpenAiModelCapabilities.questionGenerationMaxOutputTokens(ReasoningEffort.NONE, true),
          is(1000L));
    }

    @Test
    void usesHigherBudgetWhenReasoningIsEnabled() {
      assertThat(
          OpenAiModelCapabilities.questionGenerationMaxOutputTokens(ReasoningEffort.MEDIUM, false),
          is(2000L));
      assertThat(
          OpenAiModelCapabilities.questionGenerationMaxOutputTokens(ReasoningEffort.HIGH, true),
          is(4000L));
    }
  }
}
