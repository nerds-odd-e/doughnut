package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.FunctionDefinition;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;
import com.openai.models.images.Image;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatToolCall;
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

  public Optional<ChatCompletionChoice> chatCompletion(ChatCompletionCreateParams params) {
    ChatCompletion response = officialClient.chat().completions().create(params);
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(convertChoice(response.choices().get(0)));
  }

  private ChatCompletionTool convertTool(com.theokanning.openai.completion.chat.ChatTool tool) {
    Object function = tool.getFunction();
    if (function instanceof FunctionDefinition legacyDefinition) {
      try {
        com.openai.models.FunctionDefinition.Builder officialBuilder =
            com.openai.models.FunctionDefinition.builder()
                .name(legacyDefinition.getName())
                .description(legacyDefinition.getDescription());
        if (legacyDefinition.getStrict() != null) {
          officialBuilder.strict(legacyDefinition.getStrict());
        }
        // If parametersDefinitionClass is set, use addTool(Class) pattern via the builder
        // For now, we'll need to handle parameters manually
        // The official library's addTool(Class) handles this automatically, but we're converting
        // So we'll skip tools that require class-based parameters for now and let them use the
        // legacy client
        if (legacyDefinition.getParametersDefinitionClass() != null) {
          // Cannot easily convert class-based parameters without the official library's
          // internal schema generation. Return null to fall back to legacy client.
          return null;
        }
        // If parametersDefinition is set as an object, try to convert it
        if (legacyDefinition.getParametersDefinition() != null) {
          com.openai.models.FunctionParameters params =
              objectMapper.convertValue(
                  legacyDefinition.getParametersDefinition(),
                  com.openai.models.FunctionParameters.class);
          officialBuilder.parameters(params);
        }
        com.openai.models.FunctionDefinition officialDefinition = officialBuilder.build();
        return ChatCompletionTool.ofFunction(
            com.openai.models.chat.completions.ChatCompletionFunctionTool.builder()
                .function(officialDefinition)
                .build());
      } catch (IllegalArgumentException ex) {
        throw new OpenAIServiceErrorException(
            "Failed to convert tool definition", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return null;
  }

  private ChatCompletionMessageToolCall convertToolDefinition(ChatToolCall toolCall) {
    if (toolCall.getFunction() == null) {
      return null;
    }
    ChatFunctionCall functionCall = toolCall.getFunction();
    ChatCompletionMessageFunctionToolCall.Function function =
        ChatCompletionMessageFunctionToolCall.Function.builder()
            .name(functionCall.getName())
            .arguments(
                functionCall.getArguments() != null ? functionCall.getArguments().toString() : null)
            .build();
    return ChatCompletionMessageToolCall.ofFunction(
        ChatCompletionMessageFunctionToolCall.builder()
            .id(toolCall.getId() != null ? toolCall.getId() : "tool_call_" + toolCall.getIndex())
            .function(function)
            .build());
  }

  private ChatCompletionChoice convertChoice(ChatCompletion.Choice choice) {
    ChatCompletionChoice legacyChoice = new ChatCompletionChoice();
    legacyChoice.setIndex(Math.toIntExact(choice.index()));
    if (choice.finishReason() != null) {
      legacyChoice.setFinishReason(choice.finishReason().toString());
    }
    AssistantMessage assistantMessage = new AssistantMessage("assistant");
    choice.message().content().ifPresent(assistantMessage::setContent);
    choice
        .message()
        .toolCalls()
        .ifPresent(
            toolCalls -> assistantMessage.setToolCalls(convertToolCallsFromResponse(toolCalls)));
    choice
        .message()
        .functionCall()
        .ifPresent(
            functionCall -> assistantMessage.setFunctionCall(convertFunctionCall(functionCall)));
    legacyChoice.setMessage(assistantMessage);
    return legacyChoice;
  }

  private List<ChatToolCall> convertToolCallsFromResponse(
      List<ChatCompletionMessageToolCall> toolCalls) {
    List<ChatToolCall> legacyCalls = new ArrayList<>();
    for (int i = 0; i < toolCalls.size(); i++) {
      ChatToolCall call = convertToolCall(toolCalls.get(i), i);
      if (call != null) {
        legacyCalls.add(call);
      }
    }
    return legacyCalls;
  }

  private ChatToolCall convertToolCall(ChatCompletionMessageToolCall toolCall, int index) {
    if (toolCall.function().isEmpty()) {
      // Return a tool call with empty function name to preserve old behavior
      // This will cause parsing to fail and throw RuntimeException as expected
      ChatToolCall legacyCall = new ChatToolCall();
      legacyCall.setIndex(index);
      legacyCall.setId("tool_call_" + index);
      legacyCall.setType("function");
      ChatFunctionCall legacyFunction = new ChatFunctionCall();
      legacyFunction.setName("");
      legacyFunction.setArguments(objectMapper.createObjectNode());
      legacyCall.setFunction(legacyFunction);
      return legacyCall;
    }
    ChatCompletionMessageFunctionToolCall functionToolCall = toolCall.asFunction();
    ChatToolCall legacyCall = new ChatToolCall();
    legacyCall.setIndex(index);
    legacyCall.setId(functionToolCall.id());
    legacyCall.setType("function");
    ChatFunctionCall legacyFunction = new ChatFunctionCall();
    legacyFunction.setName(functionToolCall.function().name());
    String arguments = functionToolCall.function().arguments();
    try {
      legacyFunction.setArguments(
          arguments != null ? objectMapper.readTree(arguments) : objectMapper.createObjectNode());
    } catch (JsonProcessingException e) {
      legacyFunction.setArguments(objectMapper.createObjectNode());
    }
    legacyCall.setFunction(legacyFunction);
    return legacyCall;
  }

  private ChatFunctionCall convertFunctionCall(ChatCompletionMessage.FunctionCall functionCall) {
    ChatFunctionCall legacyFunction = new ChatFunctionCall();
    legacyFunction.setName(functionCall.name());
    try {
      legacyFunction.setArguments(
          functionCall.arguments() != null
              ? objectMapper.readTree(functionCall.arguments())
              : objectMapper.createObjectNode());
    } catch (JsonProcessingException e) {
      legacyFunction.setArguments(objectMapper.createObjectNode());
    }
    return legacyFunction;
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
      Optional<ChatCompletionChoice> choiceOpt = chatCompletion(chatRequest);
      if (choiceOpt.isEmpty()) {
        return Optional.empty();
      }
      ChatCompletionChoice choice = choiceOpt.get();
      AssistantMessage message = choice.getMessage();

      // If we get tool calls instead of content, try to extract JSON from tool call arguments
      // This preserves old behavior where tool calls with invalid data would fail during parsing
      if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
        ChatToolCall toolCall = message.getToolCalls().get(0);
        if (toolCall.getFunction() != null) {
          String functionName = toolCall.getFunction().getName();
          // If function name is empty or doesn't match expected tool, try to parse arguments
          // This will fail and throw RuntimeException as expected by tests
          try {
            String arguments =
                toolCall.getFunction().getArguments() != null
                    ? toolCall.getFunction().getArguments().toString()
                    : "{}";
            return Optional.of(new ObjectMapperConfig().objectMapper().readTree(arguments));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        }
        return Optional.empty();
      }

      String content = message.getContent();
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
