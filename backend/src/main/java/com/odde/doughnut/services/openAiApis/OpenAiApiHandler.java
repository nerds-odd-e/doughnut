package com.odde.doughnut.services.openAiApis;

import static com.odde.doughnut.services.openAiApis.ApiExecutor.blockGet;

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
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;
import com.openai.models.images.Image;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatTool;
import com.theokanning.openai.completion.chat.ChatToolCall;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import com.theokanning.openai.fine_tuning.FineTuningJob;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.fine_tuning.Hyperparameters;
import com.theokanning.openai.model.Model;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Handler for OpenAI API calls, supporting Chat Completion API.
 *
 * <p>Chat Completion API methods: {@link #chatCompletion}, {@link #streamChatCompletion}
 */
@Service
public class OpenAiApiHandler {
  private final OpenAiApi openAiApi;
  private final OpenAIClient officialClient;
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  @Autowired
  public OpenAiApiHandler(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      @Qualifier("officialOpenAiClient") OpenAIClient officialClient) {
    this.openAiApi = openAiApi;
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

  public Optional<ChatCompletionChoice> chatCompletion(ChatCompletionRequest request) {
    ChatCompletionCreateParams params = buildChatCompletionParams(request);
    ChatCompletion response = officialClient.chat().completions().create(params);
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(convertChoice(response.choices().get(0)));
  }

  private ChatCompletionCreateParams buildChatCompletionParams(ChatCompletionRequest request) {
    ChatCompletionCreateParams.Builder builder =
        ChatCompletionCreateParams.builder().model(ChatModel.of(request.getModel()));
    applyMessages(request.getMessages(), builder);
    applyOptionalSettings(request, builder);
    applyTools(request.getTools(), builder);
    applyResponseFormat(request.getResponseFormat(), builder);
    return builder.build();
  }

  private void applyMessages(
      List<ChatMessage> messages, ChatCompletionCreateParams.Builder builder) {
    if (messages == null) {
      return;
    }
    for (ChatMessage message : messages) {
      if (message instanceof SystemMessage systemMessage) {
        builder.addSystemMessage(systemMessage.getContent());
      } else if (message instanceof UserMessage userMessage) {
        String text = userMessage.getTextContent();
        if (text == null && userMessage.getContent() != null) {
          text = userMessage.getContent().toString();
        }
        builder.addUserMessage(text);
      } else if (message instanceof AssistantMessage assistantMessage) {
        builder.addMessage(
            ChatCompletionMessageParam.ofAssistant(buildAssistantParam(assistantMessage)));
      }
    }
  }

  private ChatCompletionAssistantMessageParam buildAssistantParam(
      AssistantMessage assistantMessage) {
    ChatCompletionAssistantMessageParam.Builder builder =
        ChatCompletionAssistantMessageParam.builder();
    if (assistantMessage.getContent() != null) {
      builder.content(assistantMessage.getContent());
    }
    if (assistantMessage.getName() != null) {
      builder.name(assistantMessage.getName());
    }
    if (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty()) {
      List<ChatCompletionMessageToolCall> toolCalls = new ArrayList<>();
      for (ChatToolCall toolCall : assistantMessage.getToolCalls()) {
        ChatCompletionMessageToolCall converted = convertToolDefinition(toolCall);
        if (converted != null) {
          toolCalls.add(converted);
        }
      }
      builder.toolCalls(toolCalls);
    }
    if (assistantMessage.getFunctionCall() != null) {
      builder.functionCall(
          ChatCompletionAssistantMessageParam.FunctionCall.builder()
              .name(assistantMessage.getFunctionCall().getName())
              .arguments(
                  assistantMessage.getFunctionCall().getArguments() != null
                      ? assistantMessage.getFunctionCall().getArguments().toString()
                      : null)
              .build());
    }
    return builder.build();
  }

  private void applyOptionalSettings(
      ChatCompletionRequest request, ChatCompletionCreateParams.Builder builder) {
    if (request.getMaxTokens() != null) {
      builder.maxCompletionTokens(request.getMaxTokens().longValue());
    }
    if (request.getTemperature() != null) {
      builder.temperature(request.getTemperature());
    }
    if (request.getTopP() != null) {
      builder.topP(request.getTopP());
    }
    if (request.getPresencePenalty() != null) {
      builder.presencePenalty(request.getPresencePenalty());
    }
    if (request.getFrequencyPenalty() != null) {
      builder.frequencyPenalty(request.getFrequencyPenalty());
    }
    if (request.getStop() != null && !request.getStop().isEmpty()) {
      builder.stopOfStrings(request.getStop());
    }
    if (request.getUser() != null) {
      builder.user(request.getUser());
    }
    if (request.getN() != null) {
      builder.n(request.getN().longValue());
    }
  }

  private void applyTools(List<ChatTool> tools, ChatCompletionCreateParams.Builder builder) {
    if (tools == null || tools.isEmpty()) {
      return;
    }
    for (ChatTool tool : tools) {
      Object function = tool.getFunction();
      if (function instanceof FunctionDefinition legacyDefinition) {
        // If parametersDefinitionClass is set, use the official library's addTool(Class) method
        if (legacyDefinition.getParametersDefinitionClass() != null) {
          @SuppressWarnings("unchecked")
          Class<Object> paramClass =
              (Class<Object>) legacyDefinition.getParametersDefinitionClass();
          builder.addTool(paramClass);
        } else {
          // Try to convert the tool with object-based parameters
          ChatCompletionTool converted = convertTool(tool);
          if (converted != null) {
            builder.addTool(converted);
          }
        }
      } else {
        // Fallback: try to convert the tool
        ChatCompletionTool converted = convertTool(tool);
        if (converted != null) {
          builder.addTool(converted);
        }
      }
    }
  }

  private void applyResponseFormat(
      com.theokanning.openai.completion.chat.ChatResponseFormat responseFormat,
      ChatCompletionCreateParams.Builder builder) {
    if (responseFormat == null) {
      return;
    }
    try {
      String type = responseFormat.getType();
      if ("json_schema".equals(type)) {
        com.theokanning.openai.completion.chat.ResponseJsonSchema legacySchema =
            responseFormat.getJsonSchema();
        if (legacySchema != null) {
          // Convert ResponseJsonSchema to official library's format
          // The official library uses responseFormat(Class) for structured output
          Class<?> schemaClass = legacySchema.getSchemaClass();
          if (schemaClass != null) {
            @SuppressWarnings("unchecked")
            Class<Object> paramClass = (Class<Object>) schemaClass;
            builder.responseFormat(paramClass);
          } else {
            // If no schema class, try to convert the schema definition
            throw new OpenAIServiceErrorException(
                "Response format requires schema class", HttpStatus.INTERNAL_SERVER_ERROR);
          }
        }
      } else if ("json_object".equals(type)) {
        builder.responseFormat(com.openai.models.ResponseFormatJsonObject.builder().build());
      } else if ("text".equals(type)) {
        builder.responseFormat(com.openai.models.ResponseFormatText.builder().build());
      }
      // "auto" type doesn't need to be set explicitly
    } catch (Exception ex) {
      throw new OpenAIServiceErrorException(
          "Failed to convert response format: " + ex.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private ChatCompletionTool convertTool(ChatTool tool) {
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

  public Flowable<String> streamChatCompletion(ChatCompletionRequest request) {
    // Build params for streaming using the official client
    ChatCompletionCreateParams.Builder builder =
        ChatCompletionCreateParams.builder().model(ChatModel.of(request.getModel()));
    applyMessages(request.getMessages(), builder);
    applyOptionalSettings(request, builder);
    applyTools(request.getTools(), builder);
    applyResponseFormat(request.getResponseFormat(), builder);
    ChatCompletionCreateParams params = builder.build();

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

  public List<Model> getModels() {
    return blockGet(openAiApi.listModels()).data;
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

  public FineTuningJob triggerFineTuning(String fileId) {
    // Fine-tuning API is not yet available in the official SDK 4.8.0
    // Keep using legacy client for now
    FineTuningJobRequest fineTuningJobRequest = new FineTuningJobRequest();
    fineTuningJobRequest.setTrainingFile(fileId);
    fineTuningJobRequest.setModel("gpt-3.5-turbo-1106");
    fineTuningJobRequest.setHyperparameters(
        new Hyperparameters()); // not sure what should be the nEpochs value

    FineTuningJob fineTuningJob = blockGet(openAiApi.createFineTuningJob(fineTuningJobRequest));
    if (List.of("failed", "cancelled").contains(fineTuningJob.getStatus())) {
      throw new OpenAIServiceErrorException(
          "Trigger Fine-Tuning Failed: " + fineTuningJob, HttpStatus.BAD_REQUEST);
    }
    return fineTuningJob;
  }

  public String getTranscription(RequestBody requestBody) throws IOException {
    return blockGet(((OpenAiApiExtended) openAiApi).createTranscriptionSrt(requestBody)).string();
  }

  public Optional<JsonNode> requestAndGetJsonSchemaResult(
      InstructionAndSchema tool, OpenAIChatRequestBuilder openAIChatRequestBuilder) {
    ChatCompletionRequest chatRequest = openAIChatRequestBuilder.responseJsonSchema(tool).build();

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
