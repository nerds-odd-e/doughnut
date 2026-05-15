package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.services.ai.ChatMessageContent;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAiApiHandler {
  private final OpenAIClient officialClient;
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
  private final TestabilitySettings testabilitySettings;
  private final String defaultOpenAiToken;

  @Autowired
  public OpenAiApiHandler(
      @Qualifier("officialOpenAiClient") OpenAIClient officialClient,
      TestabilitySettings testabilitySettings,
      @Value("${spring.openai.token}") String defaultOpenAiToken) {
    this.officialClient = officialClient;
    this.testabilitySettings = testabilitySettings;
    this.defaultOpenAiToken = defaultOpenAiToken;
  }

  private void assertOpenAiAvailable() {
    String effectiveToken =
        testabilitySettings.getOpenAiTokenOverride() != null
            ? testabilitySettings.getOpenAiTokenOverride()
            : defaultOpenAiToken;
    if (effectiveToken == null || effectiveToken.isEmpty()) {
      throw new OpenAiNotAvailableException("OpenAI is not available (no API key configured).");
    }
  }

  public Optional<ChatCompletion.Choice> chatCompletion(ChatCompletionCreateParams params) {
    assertOpenAiAvailable();
    ChatCompletion response = officialClient.chat().completions().create(params);
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(response.choices().get(0));
  }

  public Flowable<String> streamChatCompletion(ChatCompletionCreateParams params) {
    assertOpenAiAvailable();
    return Flowable.<String>create(
        emitter -> {
          // Use the async client's subscribe() method for true async streaming
          // The OpenAI SDK's async API handles threading internally, so we don't need
          // subscribeOn() which would move session-scoped bean access to a background thread
          officialClient
              .async()
              .chat()
              .completions()
              .createStreaming(params)
              .subscribe(
                  chunk -> {
                    try {
                      // Convert ChatCompletionChunk to JSON string to preserve delta field
                      // structure expected by frontend
                      String jsonString = objectMapper.writeValueAsString(chunk);
                      emitter.onNext(jsonString);
                    } catch (JsonProcessingException e) {
                      emitter.onError(new RuntimeException("Failed to serialize chunk to JSON", e));
                    }
                  })
              .onCompleteFuture()
              .whenComplete(
                  (unused, error) -> {
                    if (error != null) {
                      // Handle unauthorized errors
                      if (error.getMessage() != null
                          && (error.getMessage().contains("401")
                              || error.getMessage().contains("Unauthorized"))) {
                        emitter.onError(
                            new com.odde.doughnut.exceptions.OpenAiUnauthorizedException(
                                "Unauthorized: " + error.getMessage()));
                      } else {
                        emitter.onError(error);
                      }
                    } else {
                      emitter.onComplete();
                    }
                  });
        },
        BackpressureStrategy.BUFFER);
  }

  public List<com.openai.models.models.Model> getModels() {
    assertOpenAiAvailable();
    var modelsResponse = officialClient.models().list();
    if (modelsResponse != null && modelsResponse.data() != null) {
      return modelsResponse.data();
    }
    return new ArrayList<>();
  }

  public String getTranscription(String filename, byte[] audioBytes) throws IOException {
    assertOpenAiAvailable();
    var params =
        com.openai.models.audio.transcriptions.TranscriptionCreateParams.builder()
            .file(audioBytes)
            .model("whisper-1")
            .responseFormat(com.openai.models.audio.AudioResponseFormat.SRT)
            .build();
    var transcription = officialClient.audio().transcriptions().create(params);
    // For SRT format, TranscriptionCreateResponse contains the transcription text
    // The response object's toString() should return the transcription text for SRT format
    return transcription.toString();
  }

  public Optional<JsonNode> requestAndGetJsonSchemaResult(
      InstructionAndSchema tool, OpenAIChatRequestBuilder openAIChatRequestBuilder) {
    return requestAndGetJsonSchemaResult(tool, openAIChatRequestBuilder, null);
  }

  public Optional<JsonNode> requestAndGetJsonSchemaResult(ChatCompletionCreateParams chatRequest) {
    assertOpenAiAvailable();

    // Guard against misuse: requestAndGetJsonSchemaResult must not be used with tools
    if (chatRequest.tools().map(list -> !list.isEmpty()).orElse(false)) {
      throw new RuntimeException(
          "requestAndGetJsonSchemaResult must not be used with tools; use the conversation tooling path instead");
    }

    try {
      Optional<ChatCompletion.Choice> choiceOpt = chatCompletion(chatRequest);
      if (choiceOpt.isEmpty()) {
        return Optional.empty();
      }
      ChatCompletion.Choice choice = choiceOpt.get();
      ChatCompletionMessage message = choice.message();

      // Extract content from message
      Optional<?> contentOpt = message.content();
      if (contentOpt.isEmpty()) {
        return Optional.empty();
      }
      String content = ChatMessageContent.extractContentString(contentOpt.get());
      if (content == null || content.isEmpty()) {
        return Optional.empty();
      }

      try {
        return Optional.of(new ObjectMapperConfig().objectMapper().readTree(content));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    } catch (RuntimeException e) {
      if (e.getCause() instanceof MismatchedInputException) {
        return Optional.empty();
      }
      throw e;
    }
  }

  /**
   * Appends {@code developerInstructionAfterSchemaInstruction} to the developer message after the
   * tool/schema instruction from {@link OpenAIChatRequestBuilder#responseJsonSchema}.
   */
  public Optional<JsonNode> requestAndGetJsonSchemaResult(
      InstructionAndSchema tool,
      OpenAIChatRequestBuilder openAIChatRequestBuilder,
      String developerInstructionAfterSchemaInstruction) {
    assertOpenAiAvailable();
    OpenAIChatRequestBuilder prepared = openAIChatRequestBuilder.responseJsonSchema(tool);
    if (developerInstructionAfterSchemaInstruction != null
        && !developerInstructionAfterSchemaInstruction.isBlank()) {
      prepared.addToOverallSystemMessage(developerInstructionAfterSchemaInstruction);
    }
    ChatCompletionCreateParams chatRequest = prepared.build();
    return requestAndGetJsonSchemaResult(chatRequest);
  }
}
