package com.odde.donut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseTextConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OpenAiModelCapabilitiesTest {

  @ParameterizedTest
  @ValueSource(strings = {"o3-mini", "o4-mini", "gpt-5", "GPT-5-preview"})
  void supportsReasoningEffortForReasoningModelPrefixes(String model) {
    assertThat(OpenAiModelCapabilities.supportsReasoningEffort(model), is(true));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"gpt-4.1-mini", "gpt-4o"})
  void doesNotSupportReasoningEffortForNonReasoningModels(String model) {
    assertThat(OpenAiModelCapabilities.supportsReasoningEffort(model), is(false));
  }

  @ParameterizedTest
  @CsvSource({
    "o3-mini, false",
    "o3-mini, true",
  })
  void questionGenerationReasoningEffortWhenSupported(String model, boolean batch) {
    assertThat(
        OpenAiModelCapabilities.questionGenerationReasoningEffort(model, batch),
        is(batch ? ReasoningEffort.HIGH : ReasoningEffort.MEDIUM));
  }

  @ParameterizedTest
  @CsvSource({"gpt-4.1-mini, false", "gpt-4.1-mini, true"})
  void questionGenerationReasoningEffortNoneWhenUnsupported(String model, boolean batch) {
    assertThat(
        OpenAiModelCapabilities.questionGenerationReasoningEffort(model, batch),
        is(ReasoningEffort.NONE));
  }

  @ParameterizedTest
  @CsvSource({
    "gpt-4.1-mini, true",
    "gpt-4o, false",
  })
  void responseTextVerbosity(String model, boolean medium) {
    assertThat(
        OpenAiModelCapabilities.responseTextVerbosity(model),
        is(medium ? ResponseTextConfig.Verbosity.MEDIUM : ResponseTextConfig.Verbosity.LOW));
  }

  @ParameterizedTest
  @CsvSource({
    "false, 1000",
    "true, 1000",
  })
  void questionGenerationMaxOutputTokensWithoutReasoning(boolean batch, long expected) {
    assertThat(
        OpenAiModelCapabilities.questionGenerationMaxOutputTokens(ReasoningEffort.NONE, batch),
        is(expected));
  }

  @ParameterizedTest
  @CsvSource({
    "false, 2000",
    "true, 12000",
  })
  void questionGenerationMaxOutputTokensWithReasoning(boolean batch, long expected) {
    ReasoningEffort effort = batch ? ReasoningEffort.HIGH : ReasoningEffort.MEDIUM;
    assertThat(
        OpenAiModelCapabilities.questionGenerationMaxOutputTokens(effort, batch), is(expected));
  }
}
