package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.ChatMessageForFineTuning;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;
import com.openai.models.images.Image;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpenAiApiHandler {
  private final OpenAIClient officialClient; // Official SDK
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  @Autowired
  public OpenAiApiHandler(@Qualifier("officialOpenAiClient") OpenAIClient officialClient) {
    this.officialClient = officialClient;
  }

  public String getOpenAiImage(String prompt) {
    ImageGenerateParams params =
        ImageGenerateParams.builder()
            .prompt(prompt)
            .responseFormat(ImageGenerateParams.ResponseFormat.B64_JSON)
            .build();
    ImagesResponse response = officialClient.images().generate(params);
    Image image =
        response
            .data()
            .orElseThrow(
                () ->
                    new OpenAIServiceErrorException(
                        "Image generation returned no data", HttpStatus.INTERNAL_SERVER_ERROR))
            .get(0);
    return image
        .b64Json()
        .orElseThrow(
            () ->
                new OpenAIServiceErrorException(
                    "Image generation did not return b64_json", HttpStatus.INTERNAL_SERVER_ERROR));
  }

  public Optional<ChatCompletion.Choice> chatCompletion(ChatCompletionCreateParams params) {
    ChatCompletion response = officialClient.chat().completions().create(params);
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(response.choices().get(0));
  }

  public Flowable<String> streamChatCompletion(ChatCompletionCreateParams params) {

    return Flowable.create(
        emitter -> {
          StreamResponse<ChatCompletionChunk> streamResponse = null;
          try {
            streamResponse = officialClient.chat().completions().createStreaming(params);
            final StreamResponse<ChatCompletionChunk> finalStreamResponse = streamResponse;

            // Register cleanup when emitter is cancelled
            emitter.setCancellable(
                () -> {
                  if (finalStreamResponse != null) {
                    try {
                      finalStreamResponse.close();
                    } catch (Exception e) {
                      // Ignore errors during cleanup
                    }
                  }
                });

            streamResponse.stream()
                .forEach(
                    chunk -> {
                      try {
                        // Convert ChatCompletionChunk back to JSON string to preserve delta field
                        // structure expected by frontend
                        String jsonString = objectMapper.writeValueAsString(chunk);
                        emitter.onNext(jsonString);
                      } catch (JsonProcessingException e) {
                        emitter.onError(
                            new RuntimeException("Failed to serialize chunk to JSON", e));
                      }
                    });
            emitter.onComplete();
          } catch (Exception e) {
            // Handle unauthorized errors
            if (e.getMessage() != null
                && (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized"))) {
              emitter.onError(
                  new com.odde.doughnut.exceptions.OpenAiUnauthorizedException(
                      "Unauthorized: " + e.getMessage()));
            } else {
              emitter.onError(e);
            }
          } finally {
            if (streamResponse != null) {
              try {
                streamResponse.close();
              } catch (Exception e) {
                // Ignore errors during cleanup
              }
            }
          }
        },
        BackpressureStrategy.BUFFER);
  }

  public List<com.openai.models.models.Model> getModels() {
    // Use official SDK models API
    var modelsResponse = officialClient.models().list();
    if (modelsResponse != null && modelsResponse.data() != null) {
      return modelsResponse.data();
    }
    return new ArrayList<>();
  }

  public String uploadTextFile(String subFileName, String content, String purpose, String suffix)
      throws IOException {
    File tempFile = File.createTempFile(subFileName, suffix);
    try {
      Files.write(tempFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);
      try {
        FilePurpose filePurpose =
            "fine-tune".equals(purpose) ? FilePurpose.FINE_TUNE : FilePurpose.ASSISTANTS;
        FileCreateParams params =
            FileCreateParams.builder().purpose(filePurpose).file(tempFile.toPath()).build();
        FileObject fileObject = officialClient.files().create(params);
        return fileObject.id();
      } catch (Exception e) {
        throw new OpenAIServiceErrorException("Upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    } finally {
      tempFile.delete();
    }
  }

  public com.openai.models.finetuning.jobs.FineTuningJob triggerFineTuning(String fileId) {
    // Use official SDK fine-tuning API
    var params =
        com.openai.models.finetuning.jobs.JobCreateParams.builder()
            .trainingFile(fileId)
            .model("gpt-3.5-turbo-1106")
            .build();
    var fineTuningJob = officialClient.fineTuning().jobs().create(params);
    var status = fineTuningJob.status();
    // Check if status indicates failure
    // Handle both enum comparison and string comparison for robustness
    boolean isFailed = false;
    if (status != null) {
      if (status == com.openai.models.finetuning.jobs.FineTuningJob.Status.FAILED
          || status == com.openai.models.finetuning.jobs.FineTuningJob.Status.CANCELLED) {
        isFailed = true;
      } else {
        // Fallback: check string representation (case-insensitive)
        String statusStr = status.toString().toUpperCase();
        if ("FAILED".equals(statusStr) || "CANCELLED".equals(statusStr)) {
          isFailed = true;
        }
      }
    }
    if (isFailed) {
      throw new OpenAIServiceErrorException(
          "Trigger Fine-Tuning Failed: " + fineTuningJob, HttpStatus.BAD_REQUEST);
    }
    return fineTuningJob;
  }

  public String getTranscription(String filename, byte[] audioBytes) throws IOException {
    // Use official SDK audio transcription API
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
    ChatCompletionCreateParams chatRequest =
        openAIChatRequestBuilder.responseJsonSchema(tool).build();

    try {
      Optional<ChatCompletion.Choice> choiceOpt = chatCompletion(chatRequest);
      if (choiceOpt.isEmpty()) {
        return Optional.empty();
      }
      ChatCompletion.Choice choice = choiceOpt.get();
      ChatCompletionMessage message = choice.message();

      // If we get tool calls instead of content, try to extract JSON from tool call arguments
      // This preserves old behavior where tool calls with invalid data would fail during parsing
      Optional<List<ChatCompletionMessageToolCall>> toolCallsOpt = message.toolCalls();
      if (toolCallsOpt.isPresent() && !toolCallsOpt.get().isEmpty()) {
        ChatCompletionMessageToolCall toolCall = toolCallsOpt.get().get(0);
        if (toolCall.function().isPresent()) {
          ChatCompletionMessageFunctionToolCall functionToolCall = toolCall.asFunction();
          String functionName = functionToolCall.function().name();
          // If function name is empty or doesn't match expected tool, try to parse arguments
          // This will fail and throw RuntimeException as expected by tests
          try {
            String arguments = functionToolCall.function().arguments();
            return Optional.of(
                new ObjectMapperConfig()
                    .objectMapper()
                    .readTree(arguments != null ? arguments : "{}"));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        }
        return Optional.empty();
      }

      // Extract content from message
      Optional<?> contentOpt = message.content();
      if (contentOpt.isEmpty()) {
        return Optional.empty();
      }
      String content = ChatMessageForFineTuning.extractContentString(contentOpt.get());
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
}
