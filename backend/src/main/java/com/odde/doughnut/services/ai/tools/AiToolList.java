package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.theokanning.openai.assistants.AssistantFunction;
import com.theokanning.openai.assistants.AssistantToolsEnum;
import com.theokanning.openai.assistants.Tool;
import com.theokanning.openai.completion.chat.ChatFunction;
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
