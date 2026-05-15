package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
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

  public <T> Optional<T> requestAndGetStructuredResponseResult(
      StructuredResponseCreateParams<T> responseRequest) {
    assertOpenAiAvailable();
    StructuredResponse<T> response = officialClient.responses().create(responseRequest);
    if (response == null || response.output() == null || response.output().isEmpty()) {
      return Optional.empty();
    }

    return response.output().stream()
        .flatMap(output -> output.message().stream())
        .flatMap(message -> message.content().stream())
        .flatMap(content -> content.outputText().stream())
        .findFirst();
  }
}
