package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import java.util.function.Function;

public class AiNoteAutomationService {
  private final OpenAiApiHandler openAiApiHandler;
  private final GlobalSettingsService globalSettingsService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  private final Note note;

  public AiNoteAutomationService(
      OpenAiApiHandler openAiApiHandler,
      GlobalSettingsService globalSettingsService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      Note note) {
    this.openAiApiHandler = openAiApiHandler;
    this.globalSettingsService = globalSettingsService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
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

  public PointExtractionResult promotePointToSibling(String point) throws JsonProcessingException {
    String t = note.getTitle() != null ? note.getTitle() : "";
    String d = note.getContent() != null ? note.getContent() : "";
    InstructionAndSchema tool = AiToolFactory.promotePointToSiblingAiTool(point, t, d);
    return executeWithTool(tool, PointExtractionResult.class, result -> result, null);
  }

  private <T, R> R executeWithTool(
      InstructionAndSchema tool, Class<T> resultClass, Function<T, R> extractor, R defaultValue) {
    StructuredResponseCreateParams<T> params = buildStructuredResponseParams(resultClass, tool);
    return openAiApiHandler
        .requestAndGetStructuredResponseResult(params)
        .map(extractor)
        .orElse(defaultValue);
  }

  private <T> StructuredResponseCreateParams<T> buildStructuredResponseParams(
      Class<T> resultClass, InstructionAndSchema tool) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    RetrievalConfig config = RetrievalConfig.defaultMaxDepth();
    FocusContextResult focusContextResult = focusContextRetrievalService.retrieve(note, config);
    String focusMarkdown = focusContextMarkdownRenderer.render(focusContextResult, config);

    OpenAIResponseRequestBuilder<T> builder =
        new OpenAIResponseRequestBuilder<>(resultClass).model(modelName);
    builder.addInstruction(OpenAIChatRequestBuilder.systemInstruction);
    builder.addInstruction(focusMarkdown);
    String notebookAssistant = note.getNotebookAssistantInstructions();
    if (notebookAssistant != null && !notebookAssistant.trim().isEmpty()) {
      builder.addInstruction(notebookAssistant);
    }
    builder.addInstruction(tool.getMessageBody());
    return builder.build();
  }

  public String removePointsAndRegenerateContent(List<String> pointsToRemove)
      throws JsonProcessingException {
    if (pointsToRemove == null || pointsToRemove.isEmpty()) {
      return note.getContent();
    }
    return executeWithTool(
        AiToolFactory.removePointsFromContentAiTool(pointsToRemove),
        RegeneratedNoteContent.class,
        r -> r.content,
        note.getContent());
  }
}
