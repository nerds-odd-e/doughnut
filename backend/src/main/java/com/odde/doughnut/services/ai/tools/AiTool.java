package com.odde.doughnut.services.ai.tools;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;
import static com.odde.doughnut.services.ai.tools.AiToolFactory.COMPLETE_NOTE_DETAILS;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.odde.doughnut.controllers.json.AiCompletionResponse;
import com.odde.doughnut.controllers.json.ClarifyingQuestionRequiredAction;
import com.odde.doughnut.services.ai.ClarifyingQuestion;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.theokanning.openai.assistants.AssistantFunction;
import com.theokanning.openai.assistants.AssistantToolsEnum;
import com.theokanning.openai.assistants.Tool;
import com.theokanning.openai.runs.ToolCall;
import com.theokanning.openai.runs.ToolCallFunction;
import java.util.Map;
import java.util.stream.Stream;

public record AiTool(String name, String description, Class<?> parameterClass) {
  public Tool getTool() {
    return new Tool(
        AssistantToolsEnum.FUNCTION,
        AssistantFunction.builder()
            .name(name)
            .description(description)
            .parameters(serializeClassSchema(parameterClass))
            .build());
  }

  private static Map<String, Object> serializeClassSchema(Class<?> value) {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
    JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(value);
    JsonNode jsonNode = objectMapper.valueToTree(jsonSchema);
    return objectMapper.convertValue(jsonNode, Map.class);
  }

  public Stream<AiCompletionResponse> tryConsume(ToolCall toolCall) {
    ToolCallFunction function = toolCall.getFunction();
    if (!name.equals(function.getName())) {
      return Stream.empty();
    }

    if (function.getName().equals(askClarificationQuestion)) {
      ClarifyingQuestion result1 = (ClarifyingQuestion) convertArguments(function);
      AiCompletionResponse result = new AiCompletionResponse();
      ClarifyingQuestionRequiredAction cqra = new ClarifyingQuestionRequiredAction();
      cqra.clarifyingQuestion = result1;
      cqra.toolCallId = toolCall.getId();

      result.setClarifyingQuestionRequiredAction(cqra);
      return Stream.of(result);
    } else if (function.getName().equals(COMPLETE_NOTE_DETAILS)) {
      NoteDetailsCompletion noteDetailsCompletion =
          (NoteDetailsCompletion) convertArguments(function);
      AiCompletionResponse result = new AiCompletionResponse();
      result.setMoreCompleteContent(noteDetailsCompletion.completion);
      return Stream.of(result);
    }
    return Stream.empty();
  }

  public Object convertArguments(ToolCallFunction function) {
    String arguments = function.getArguments();
    try {
      JsonNode jsonNode = defaultObjectMapper().readTree(arguments);
      return defaultObjectMapper().treeToValue(jsonNode, parameterClass);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
