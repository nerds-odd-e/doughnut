package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
import com.openai.core.MultipartField;
import com.openai.core.http.HttpResponse;
import com.openai.models.batches.Batch;
import com.openai.models.batches.BatchCreateParams;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

  public Flowable<String> streamResponseAsLegacyChatChunks(ResponseCreateParams params) {
    assertOpenAiAvailable();
    ResponseStreamToLegacyChatChunkMapper mapper =
        new ResponseStreamToLegacyChatChunkMapper(objectMapper);
    return Flowable.create(
        emitter -> {
          officialClient
              .async()
              .responses()
              .createStreaming(params)
              .subscribe(
                  event -> {
                    try {
                      for (String json : mapper.map(event)) {
                        emitter.onNext(json);
                      }
                    } catch (JsonProcessingException e) {
                      emitter.onError(
                          new RuntimeException("Failed to serialize legacy chat chunk", e));
                    } catch (RuntimeException e) {
                      emitter.onError(e);
                    }
                  })
              .onCompleteFuture()
              .whenComplete(
                  (unused, error) -> {
                    if (error != null) {
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

  public String uploadBatchInputFile(byte[] jsonlBytes) {
    assertOpenAiAvailable();
    FileCreateParams params =
        FileCreateParams.builder()
            .purpose(FilePurpose.BATCH)
            .file(
                MultipartField.<InputStream>builder()
                    .value(new ByteArrayInputStream(jsonlBytes))
                    .filename("batch-input.jsonl")
                    .build())
            .build();
    FileObject fileObject = officialClient.files().create(params);
    return fileObject.id();
  }

  public String createResponsesBatch(String inputFileId) {
    assertOpenAiAvailable();
    BatchCreateParams params =
        BatchCreateParams.builder()
            .inputFileId(inputFileId)
            .endpoint(BatchCreateParams.Endpoint.V1_RESPONSES)
            .completionWindow(BatchCreateParams.CompletionWindow._24H)
            .build();
    Batch batch = officialClient.batches().create(params);
    return batch.id();
  }

  public Batch retrieveBatch(String batchId) {
    assertOpenAiAvailable();
    return officialClient.batches().retrieve(batchId);
  }

  public String downloadFileContent(String fileId) {
    assertOpenAiAvailable();
    try (HttpResponse response = officialClient.files().content(fileId)) {
      return new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to download OpenAI file " + fileId, e);
    }
  }

  public <T> Optional<T> parseStructuredOutputFromBatchSuccessLine(
      String rawSuccessLine, Class<T> responseType) {
    if (rawSuccessLine == null || rawSuccessLine.isBlank()) {
      return Optional.empty();
    }
    try {
      JsonNode bodyNode = objectMapper.readTree(rawSuccessLine).path("response").path("body");
      if (bodyNode.isMissingNode() || bodyNode.isNull()) {
        return Optional.empty();
      }
      for (JsonNode outputItem : bodyNode.path("output")) {
        if (!"message".equals(outputItem.path("type").asText())) {
          continue;
        }
        for (JsonNode content : outputItem.path("content")) {
          if (!"output_text".equals(content.path("type").asText())) {
            continue;
          }
          String text = content.path("text").asText(null);
          if (text == null || text.isBlank()) {
            continue;
          }
          return Optional.of(objectMapper.readValue(text, responseType));
        }
      }
      return Optional.empty();
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }
}
