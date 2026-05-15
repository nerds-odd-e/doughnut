package com.odde.doughnut.services.ai.builder;

import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.StructuredResponseTextConfig;
import java.util.ArrayList;
import java.util.List;

public class OpenAIResponseRequestBuilder<T> {
  private static final String MESSAGE_SEPARATOR = "\n\n\n";

  private final Class<T> responseType;
  private final List<String> instructions = new ArrayList<>();
  private final List<String> inputMessages = new ArrayList<>();
  private String modelName;
  private ReasoningEffort reasoningEffort = ReasoningEffort.NONE;
  private long maxOutputTokens = 700L;
  private ResponseTextConfig.Verbosity verbosity = ResponseTextConfig.Verbosity.LOW;

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
    StructuredResponseTextConfig<T> textConfig =
        StructuredResponseTextConfig.<T>builder().format(responseType).verbosity(verbosity).build();

    return StructuredResponseCreateParams.<T>builder()
        .model(modelName)
        .instructions(buildInstructions())
        .input(buildInput())
        .reasoning(Reasoning.builder().effort(reasoningEffort).build())
        .maxOutputTokens(maxOutputTokens)
        .text(textConfig)
        .build();
  }

  private String buildInstructions() {
    return String.join(MESSAGE_SEPARATOR, instructions);
  }

  private String buildInput() {
    return String.join(MESSAGE_SEPARATOR, inputMessages);
  }
}
