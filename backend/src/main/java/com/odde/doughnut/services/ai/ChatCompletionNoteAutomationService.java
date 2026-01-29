package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.util.List;
import java.util.function.Function;

public class ChatCompletionNoteAutomationService {
  private final OpenAiApiHandler openAiApiHandler;
  private final GlobalSettingsService globalSettingsService;
  private final Note note;

  public ChatCompletionNoteAutomationService(
      OpenAiApiHandler openAiApiHandler, GlobalSettingsService globalSettingsService, Note note) {
    this.openAiApiHandler = openAiApiHandler;
    this.globalSettingsService = globalSettingsService;
    this.note = note;
  }

  public String suggestTitle() throws JsonProcessingException {
    return executeWithTool(
        AiToolFactory.suggestNoteTitleAiTool(),
        TitleReplacement.class,
        TitleReplacement::getNewTitle,
        null);
  }

  public List<String> generateUnderstandingChecklist() throws JsonProcessingException {
    return executeWithTool(
        AiToolFactory.generateUnderstandingChecklistAiTool(),
        UnderstandingChecklist.class,
        UnderstandingChecklist::getPoints,
        List.of());
  }

  public PointExtractionResult promotePoint(String point) throws JsonProcessingException {
    return executeWithTool(
        AiToolFactory.promotePointAiTool(point),
        PointExtractionResult.class,
        result -> result,
        null);
  }

  private <T, R> R executeWithTool(
      InstructionAndSchema tool, Class<T> resultClass, Function<T, R> extractor, R defaultValue)
      throws JsonProcessingException {
    OpenAIChatRequestBuilder chatRequestBuilder = createChatRequestBuilder();
    chatRequestBuilder.responseJsonSchema(tool);

    return openAiApiHandler
        .requestAndGetJsonSchemaResult(tool, chatRequestBuilder)
        .map(
            jsonNode -> {
              try {
                T result =
                    new ObjectMapperConfig().objectMapper().treeToValue(jsonNode, resultClass);
                return extractor.apply(result);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .orElse(defaultValue);
  }

  private OpenAIChatRequestBuilder createChatRequestBuilder() {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    OpenAIChatRequestBuilder chatRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addToOverallSystemMessage(instructions);
    }

    return chatRequestBuilder;
  }

  public String regenerateDetailsFromPoints(List<String> points) throws JsonProcessingException {
    if (points == null || points.isEmpty()) {
      return "";
    }
    return executeWithTool(
        AiToolFactory.regenerateDetailsFromPointsAiTool(points),
        RegeneratedNoteDetails.class,
        r -> r.details,
        "");
  }

  public String removePointsAndRegenerateDetails(List<String> pointsToRemove)
      throws JsonProcessingException {
    if (pointsToRemove == null || pointsToRemove.isEmpty()) {
      return note.getDetails();
    }
    return executeWithTool(
        AiToolFactory.removePointsFromDetailsAiTool(pointsToRemove),
        RegeneratedNoteDetails.class,
        r -> r.details,
        note.getDetails());
  }
}
