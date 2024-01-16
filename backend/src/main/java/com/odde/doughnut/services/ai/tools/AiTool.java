package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.theokanning.openai.assistants.AssistantFunction;
import com.theokanning.openai.assistants.AssistantToolsEnum;
import com.theokanning.openai.assistants.Tool;
import java.util.Map;

public record AiTool(String completeNoteDetails, String description, Class<?> parameters) {
  public Tool getTool() {
    return new Tool(
        AssistantToolsEnum.FUNCTION,
        AssistantFunction.builder()
            .name(completeNoteDetails)
            .description(description)
            .parameters(serializeClassSchema(parameters))
            .build());
  }

  private static Map<String, Object> serializeClassSchema(Class<?> value) {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
    JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(value);
    JsonNode jsonNode = objectMapper.valueToTree(jsonSchema);
    return objectMapper.convertValue(jsonNode, Map.class);
  }
}
