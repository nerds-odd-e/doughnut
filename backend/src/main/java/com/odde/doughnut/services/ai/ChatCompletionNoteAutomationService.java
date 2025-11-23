package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatCompletionNoteAutomationService {
  private final OpenAiApiHandler openAiApiHandler;
  private final GlobalSettingsService globalSettingsService;
  private final ObjectMapper objectMapper;
  private final Note note;

  public ChatCompletionNoteAutomationService(
      OpenAiApiHandler openAiApiHandler,
      GlobalSettingsService globalSettingsService,
      ObjectMapper objectMapper,
      Note note) {
    this.openAiApiHandler = openAiApiHandler;
    this.globalSettingsService = globalSettingsService;
    this.objectMapper = objectMapper;
    this.note = note;
  }

  public String suggestTitle() throws JsonProcessingException {
    String instructions =
        "Please suggest a better title for the note by calling the function. Don't change it if it's already good enough.";

    // Build messages with note context
    List<ChatCompletionMessageParam> messages = buildMessages(instructions);

    // Get the suggest title tool
    Class<?> toolClass = AiToolFactory.suggestNoteTitle();

    // Create chat completion request
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    @SuppressWarnings("unchecked")
    Class<Object> paramClass = (Class<Object>) toolClass;
    ChatCompletionCreateParams request =
        ChatCompletionCreateParams.builder()
            .model(ChatModel.of(modelName))
            .messages(messages)
            .addTool(paramClass)
            .build();

    // Make non-streaming call
    try {
      return openAiApiHandler
          .chatCompletion(request)
          .map(
              choice -> {
                ChatCompletionMessage message = choice.message();
                Optional<List<ChatCompletionMessageToolCall>> toolCallsOpt = message.toolCalls();
                if (toolCallsOpt.isPresent() && !toolCallsOpt.get().isEmpty()) {
                  ChatCompletionMessageToolCall toolCall = toolCallsOpt.get().get(0);
                  if (toolCall.function().isPresent()) {
                    ChatCompletionMessageFunctionToolCall functionToolCall = toolCall.asFunction();
                    String arguments = functionToolCall.function().arguments();
                    try {
                      JsonNode argNode = parseArguments(arguments);
                      return argNode.get("newTitle").asText();
                    } catch (JsonProcessingException e) {
                      throw new RuntimeException(e);
                    }
                  }
                }
                return null;
              })
          .orElse(null);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof JsonProcessingException) {
        throw (JsonProcessingException) e.getCause();
      }
      throw e;
    }
  }

  private List<ChatCompletionMessageParam> buildMessages(String instructions) {
    List<ChatCompletionMessageParam> messages = new ArrayList<>();

    // Add note context as system message
    String noteDescription = note.getGraphRAGDescription(objectMapper);
    messages.add(
        ChatCompletionMessageParam.ofSystem(
            ChatCompletionSystemMessageParam.builder()
                .content(noteDescription + "\n\n" + note.getNotebookAssistantInstructions())
                .build()));

    // Add user instruction
    messages.add(
        ChatCompletionMessageParam.ofUser(
            ChatCompletionUserMessageParam.builder().content(instructions).build()));

    return messages;
  }

  private JsonNode parseArguments(String arguments) throws JsonProcessingException {
    return objectMapper.readTree(arguments != null ? arguments : "{}");
  }
}
