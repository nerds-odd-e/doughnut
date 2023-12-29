package com.odde.doughnut.services.ai.tools;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.theokanning.openai.assistants.AssistantFunction;
import com.theokanning.openai.assistants.AssistantToolsEnum;
import com.theokanning.openai.assistants.Tool;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.*;

public class AiToolList {
  final Map<String, ChatFunction> functions = new HashMap<>();
  private String messageBody;

  public AiToolList(String message, List<ChatFunction> functions) {
    this.messageBody = message;
    functions.forEach(f -> this.functions.put(f.getName(), f));
  }

  public String getFirstFunctionName() {
    return functions.keySet().iterator().next();
  }

  public static ChatMessage functionCall(String functionName, Object arguments) {
    ChatMessage functionCallMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value());
    functionCallMessage.setFunctionCall(
        new ChatFunctionCall(functionName, defaultObjectMapper().valueToTree(arguments)));
    return functionCallMessage;
  }

  public static ChatMessage functionCallResponse(String functionName, Object resp) {
    JsonNode jsonNode = defaultObjectMapper().convertValue(resp, JsonNode.class);
    return new ChatMessage(
        ChatMessageRole.FUNCTION.value(), jsonNode.toPrettyString(), functionName);
  }

  public void addToChat(OpenAIChatRequestBuilder openAIChatRequestBuilder) {
    openAIChatRequestBuilder.functions.addAll(functions.values());
    openAIChatRequestBuilder.addUserMessage(messageBody);
  }

  public List<Tool> getTools() {
    List<Tool> toolList = new ArrayList<>();
    functions
        .values()
        .forEach(
            f -> {
              AssistantFunction function =
                  AssistantFunction.builder()
                      .name(f.getName())
                      .description(f.getDescription())
                      .parameters(serializeClassSchema(f.getParametersClass()))
                      .build();
              Tool tool = new Tool(AssistantToolsEnum.FUNCTION, function);
              toolList.add(tool);
            });
    return toolList;
  }

  private Map<String, Object> serializeClassSchema(Class<?> value) {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
    JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(value);
    JsonNode jsonNode = objectMapper.valueToTree(jsonSchema);
    return objectMapper.convertValue(jsonNode, Map.class);
  }
}
