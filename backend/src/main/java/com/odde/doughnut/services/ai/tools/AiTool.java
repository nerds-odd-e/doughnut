package com.odde.doughnut.services.ai.tools;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.odde.doughnut.controllers.dto.AiCompletionRequiredAction;
import com.theokanning.openai.assistants.assistant.FunctionTool;
import com.theokanning.openai.function.FunctionDefinition;
import com.theokanning.openai.assistants.assistant.Tool;
import com.theokanning.openai.assistants.run.ToolCall;
import com.theokanning.openai.assistants.run.ToolCallFunction;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public record AiTool(
    String name,
    String description,
    Class<?> parameterClass,
    Function<Object, AiCompletionRequiredAction> executor) {
  public static <T> AiTool build(
      String name,
      String description,
      Class<T> parameterClass,
      Function<T, AiCompletionRequiredAction> executor) {
    return new AiTool(
        name, description, parameterClass, (arguments) -> executor.apply((T) arguments));
  }

  public Tool getTool() {
    return new FunctionTool(
      FunctionDefinition.builder()
            .name(name)
            .description(description)
        .parametersDefinition(serializeClassSchema(parameterClass))
            .build());
  }

  private static Map<String, Object> serializeClassSchema(Class<?> value) {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
    JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(value);
    JsonNode jsonNode = objectMapper.valueToTree(jsonSchema);
    return objectMapper.convertValue(jsonNode, Map.class);
  }

  public Stream<AiCompletionRequiredAction> tryConsume(ToolCall toolCall) {
    ToolCallFunction function = toolCall.getFunction();
    if (name.equals(function.getName())) {
      return Stream.of(executor.apply(convertArguments(function)));
    }
    return Stream.empty();
  }

  private Object convertArguments(ToolCallFunction function) {
    JsonNode jsonNode = function.getArguments();
    try {
      return defaultObjectMapper().treeToValue(jsonNode, parameterClass);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
