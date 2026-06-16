package com.odde.doughnut.services.ai.builder;

import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.StructuredResponseTextConfig;
import java.util.ArrayList;
import java.util.List;

public class OpenAIResponseRequestBuilder<T> {
  public static final String systemInstruction =
      "This is a PKM system: wiki-style Markdown notes in notebooks, with [[wiki links]] between notes.";

  private static final String MESSAGE_SEPARATOR = "\n\n\n";

  private final Class<T> responseType;
  private final List<String> instructions = new ArrayList<>();
  private final List<String> inputMessages = new ArrayList<>();
  private String modelName;
  private ReasoningEffort reasoningEffort = ReasoningEffort.NONE;
  private long maxOutputTokens = 700L;

  public OpenAIResponseRequestBuilder(Class<T> responseType) {
    this.responseType = responseType;
  }

  public OpenAIResponseRequestBuilder<T> model(String modelName) {
    this.modelName = modelName;
    return this;
  }

  public OpenAIResponseRequestBuilder<T> addInstruction(String instruction) {
    instructions.add(instruction);
    return this;
  }

  public OpenAIResponseRequestBuilder<T> addUserMessage(String message) {
    inputMessages.add(message);
    return this;
  }

  public OpenAIResponseRequestBuilder<T> reasoningEffort(ReasoningEffort effort) {
    reasoningEffort = effort;
    return this;
  }

  public OpenAIResponseRequestBuilder<T> maxOutputTokens(long tokens) {
    maxOutputTokens = tokens;
    return this;
  }

  public StructuredResponseCreateParams<T> build() {
    return createParams(ResponseTextConfig.Verbosity.LOW, true);
  }

  /** Batch API requests omit reasoning and use medium verbosity for model compatibility. */
  public StructuredResponseCreateParams<T> buildForBatchApi() {
    return createParams(ResponseTextConfig.Verbosity.MEDIUM, false);
  }

  private StructuredResponseCreateParams<T> createParams(
      ResponseTextConfig.Verbosity verbosity, boolean includeReasoning) {
    StructuredResponseTextConfig<T> textConfig =
        StructuredResponseTextConfig.<T>builder().format(responseType).verbosity(verbosity).build();

    StructuredResponseCreateParams.Builder<T> paramsBuilder =
        StructuredResponseCreateParams.<T>builder()
            .model(modelName)
            .instructions(buildInstructions())
            .input(buildInput())
            .maxOutputTokens(maxOutputTokens)
            .text(textConfig);
    if (includeReasoning) {
      paramsBuilder.reasoning(Reasoning.builder().effort(reasoningEffort).build());
    }
    return paramsBuilder.build();
  }

  private String buildInstructions() {
    return String.join(MESSAGE_SEPARATOR, instructions);
  }

  private String buildInput() {
    return String.join(MESSAGE_SEPARATOR, inputMessages);
  }
}
