package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.validators.DisplayNamePathSeparators;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import java.util.function.Function;

public class AiNoteAutomationService {
  private static final String STRUCTURED_RESPONSE_INPUT =
      "Follow the instructions and return the requested structured response.";
  private static final long REFINEMENT_SUGGESTIONS_MAX_OUTPUT_TOKENS = 1000L;
  private static final long REMOVE_SUGGESTIONS_MAX_OUTPUT_TOKENS = 2000L;
  private static final long EXTRACT_NOTE_MAX_OUTPUT_TOKENS = 3000L;

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
    return DisplayNamePathSeparators.toFullwidthPathSeparators(
        executeWithTool(
            AiToolFactory.suggestNoteTitleAiTool(),
            TitleReplacement.class,
            TitleReplacement::getNewTitle,
            null));
  }

  public NoteRefinementLayout generateRefinementSuggestions() throws JsonProcessingException {
    return executeWithTool(
        AiToolFactory.generateRefinementSuggestionsAiTool(),
        NoteRefinementLayout.class,
        NoteRefinementLayoutValidator::validOrEmpty,
        NoteRefinementLayout.empty(),
        REFINEMENT_SUGGESTIONS_MAX_OUTPUT_TOKENS);
  }

  public NoteExtractionResult extractNote(NoteRefinementLayout layout, List<String> selectedItemIds)
      throws JsonProcessingException {
    InstructionAndSchema tool = AiToolFactory.extractNoteAiTool(layout, selectedItemIds);
    NoteExtractionResult result =
        executeWithTool(
            tool, NoteExtractionResult.class, r -> r, null, EXTRACT_NOTE_MAX_OUTPUT_TOKENS);
    return sanitizeExtractionResult(result);
  }

  private static NoteExtractionResult sanitizeExtractionResult(NoteExtractionResult result) {
    if (result == null) {
      return null;
    }
    result.newNoteTitle = DisplayNamePathSeparators.toFullwidthPathSeparators(result.newNoteTitle);
    result.newNoteContent =
        WikiLinkMarkdown.sanitizePathSeparatorsInWikiLinks(result.newNoteContent);
    result.updatedParentContent =
        WikiLinkMarkdown.sanitizePathSeparatorsInWikiLinks(result.updatedParentContent);
    return result;
  }

  private <T, R> R executeWithTool(
      InstructionAndSchema tool, Class<T> resultClass, Function<T, R> extractor, R defaultValue) {
    StructuredResponseCreateParams<T> params =
        buildStructuredResponseParams(resultClass, tool, null);
    return executeWithParams(params, extractor, defaultValue);
  }

  private <T, R> R executeWithTool(
      InstructionAndSchema tool,
      Class<T> resultClass,
      Function<T, R> extractor,
      R defaultValue,
      long maxOutputTokens) {
    StructuredResponseCreateParams<T> params =
        buildStructuredResponseParams(resultClass, tool, maxOutputTokens);
    return executeWithParams(params, extractor, defaultValue);
  }

  private <T, R> R executeWithParams(
      StructuredResponseCreateParams<T> params, Function<T, R> extractor, R defaultValue) {
    return openAiApiHandler
        .requestAndGetStructuredResponseResult(params)
        .map(extractor)
        .orElse(defaultValue);
  }

  private <T> StructuredResponseCreateParams<T> buildStructuredResponseParams(
      Class<T> resultClass, InstructionAndSchema tool, Long maxOutputTokens) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    RetrievalConfig config = RetrievalConfig.defaultMaxDepth();
    FocusContextResult focusContextResult = focusContextRetrievalService.retrieve(note, config);
    String focusMarkdown = focusContextMarkdownRenderer.render(focusContextResult, config);

    OpenAIResponseRequestBuilder<T> builder =
        new OpenAIResponseRequestBuilder<>(resultClass).model(modelName);
    builder.addInstruction(OpenAIResponseRequestBuilder.systemInstruction);
    builder.addInstruction(focusMarkdown);
    if (maxOutputTokens != null) {
      builder.maxOutputTokens(maxOutputTokens);
    }
    builder.addInstruction(tool.getMessageBody());
    builder.addUserMessage(STRUCTURED_RESPONSE_INPUT);
    return builder.build();
  }

  public String removeSuggestionsAndRegenerateContent(List<String> suggestionsToRemove)
      throws JsonProcessingException {
    if (suggestionsToRemove == null || suggestionsToRemove.isEmpty()) {
      return note.getContent();
    }
    return executeWithTool(
        AiToolFactory.removeSuggestionsFromContentAiTool(suggestionsToRemove),
        RegeneratedNoteContent.class,
        r -> r.content,
        note.getContent(),
        REMOVE_SUGGESTIONS_MAX_OUTPUT_TOKENS);
  }
}
