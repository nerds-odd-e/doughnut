package com.odde.donut.services.openAiApis;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.configs.OpenAiProperties;
import com.odde.donut.exceptions.OpenAiNotAvailableException;
import com.odde.donut.exceptions.OpenAiUnauthorizedException;
import com.odde.donut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
import com.openai.core.MultipartField;
import com.openai.core.http.HttpResponse;
import com.openai.models.audio.AudioResponseFormat;
import com.openai.models.audio.transcriptions.Transcription;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.batches.Batch;
import com.openai.models.batches.BatchCreateParams;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OpenAiApiHandler {
  private final OpenAIClient officialClient;
  private static final JsonMapper objectMapper = ObjectMapperConfig.handWrittenJsonMapper();
  private final TestabilitySettings testabilitySettings;
  private final String defaultOpenAiToken;

  @Autowired
  public OpenAiApiHandler(
      @Qualifier("officialOpenAiClient") OpenAIClient officialClient,
      TestabilitySettings testabilitySettings,
      OpenAiProperties openAiProperties) {
    this.officialClient = officialClient;
    this.testabilitySettings = testabilitySettings;
    this.defaultOpenAiToken = openAiProperties.token();
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

  public interface ChunkListener {
    void onChunk(String json);

    void onError(Throwable error);

    void onComplete();
  }

  public void streamResponseAsLegacyChatChunks(
      ResponseCreateParams params, ChunkListener listener) {
    assertOpenAiAvailable();
    ResponseStreamToLegacyChatChunkMapper mapper = new ResponseStreamToLegacyChatChunkMapper();
    officialClient
        .async()
        .responses()
        .createStreaming(params)
        .subscribe(event -> {
          try {
            for (String json : mapper.map(event)) {
              listener.onChunk(json);
            }
          } catch (RuntimeException e) {
            listener.onError(e);
          }
        })
        .onCompleteFuture()
        .whenComplete((unused, error) -> {
          if (error == null) {
            listener.onComplete();
          } else if (error.getMessage() != null
              && (error.getMessage().contains("401")
                  || error.getMessage().contains("Unauthorized"))) {
            listener.onError(
                new OpenAiUnauthorizedException("Unauthorized: " + error.getMessage()));
          } else {
            listener.onError(error);
          }
        });
  }

  public List<com.openai.models.models.Model> getModels() {
    assertOpenAiAvailable();
    var modelsResponse = officialClient.models().list();
    if (modelsResponse != null && modelsResponse.data() != null) {
      return modelsResponse.data();
    }
    return new ArrayList<>();
  }

  public String getTranscription(
      String filename,
      byte[] audioBytes,
      String model,
      AudioResponseFormat responseFormat,
      Optional<String> prompt)
      throws IOException {
    assertOpenAiAvailable();
    String effectiveFilename = filename != null && !filename.isBlank() ? filename : "audio.wav";
    var paramsBuilder =
        TranscriptionCreateParams.builder()
            .file(
                MultipartField.<InputStream>builder()
                    .value(new ByteArrayInputStream(audioBytes))
                    .filename(effectiveFilename)
                    .build())
            .model(model)
            .responseFormat(responseFormat);
    prompt.ifPresent(paramsBuilder::prompt);
    TranscriptionCreateParams params = paramsBuilder.build();
    var transcription = officialClient.audio().transcriptions().create(params);
    return transcription
        .transcription()
        .map(Transcription::text)
        .filter(text -> !text.isBlank())
        .orElseThrow(() -> new IOException("Empty transcription response from OpenAI"));
  }

  public <T> StructuredResponseReading<T> readStructuredResponse(
      StructuredResponseCreateParams<T> responseRequest) {
    assertOpenAiAvailable();
    return StructuredResponseReading.read(officialClient.responses().create(responseRequest));
  }

  public <T> Optional<T> requestAndGetStructuredResponseResult(
      StructuredResponseCreateParams<T> responseRequest) {
    return readStructuredResponse(responseRequest).value();
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

  public record BatchStructuredOutput<T>(T output, JsonNode metadata) {}

  public <T> Optional<BatchStructuredOutput<T>> parseStructuredOutputFromBatchSuccessLine(
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
        if (!"message".equals(outputItem.path("type").asString())) {
          continue;
        }
        for (JsonNode content : outputItem.path("content")) {
          if (!"output_text".equals(content.path("type").asString())) {
            continue;
          }
          String text = content.path("text").asString(null);
          if (text == null || text.isBlank()) {
            continue;
          }
          return Optional.ofNullable(objectMapper.readValue(text, responseType))
              .map(output -> new BatchStructuredOutput<>(output, bodyNode.path("metadata")));
        }
      }
      return Optional.empty();
    } catch (JacksonException e) {
      return Optional.empty();
    }
  }
}
