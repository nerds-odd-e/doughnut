package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.completion.chat.*;
import java.util.ArrayList;
import java.util.List;

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
    List<ChatMessage> messages = buildMessages(instructions);

    // Get the suggest title tool
    AiTool tool = AiToolFactory.suggestNoteTitle();
    ChatTool chatTool = tool.getChatTool();

    // Create chat completion request
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model(modelName)
            .messages(messages)
            .tools(List.of(chatTool))
            .build();

    // Make non-streaming call
    try {
      return openAiApiHandler
          .chatCompletion(request)
          .map(ChatCompletionChoice::getMessage)
          .map(AssistantMessage::getToolCalls)
          .filter(toolCalls -> toolCalls != null && !toolCalls.isEmpty())
          .map(toolCalls -> toolCalls.get(0))
          .map(ChatToolCall::getFunction)
          .map(ChatFunctionCall::getArguments)
          .map(
              args -> {
                try {
                  return parseArguments(args);
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(e);
                }
              })
          .map(argNode -> argNode.get("newTitle"))
          .map(JsonNode::asText)
          .orElse(null);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof JsonProcessingException) {
        throw (JsonProcessingException) e.getCause();
      }
      throw e;
    }
  }

  private List<ChatMessage> buildMessages(String instructions) {
    List<ChatMessage> messages = new ArrayList<>();

    // Add note context as system message
    String noteDescription = note.getGraphRAGDescription(objectMapper);
    messages.add(
        new SystemMessage(noteDescription + "\n\n" + note.getNotebookAssistantInstructions()));

    // Add user instruction
    messages.add(new UserMessage(instructions));

    return messages;
  }

  private JsonNode parseArguments(Object arguments) throws JsonProcessingException {
    if (arguments instanceof String) {
      return objectMapper.readTree((String) arguments);
    } else if (arguments instanceof JsonNode) {
      return (JsonNode) arguments;
    } else {
      return objectMapper.valueToTree(arguments);
    }
  }
}
