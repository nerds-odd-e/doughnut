package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.ai.OpenAiModelCapabilities;
import com.odde.donut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.donut.services.ai.tools.AiToolFactory;
import com.odde.donut.services.ai.tools.InstructionAndSchema;
import com.odde.donut.services.focusContext.FocusContextConstants;
import com.odde.donut.services.focusContext.FocusContextMarkdownAugmenter;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextResult;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.RetrievalConfig;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionGenerationRequestBuilder {
  static final String CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER = "Custom instruction for focus note:";

  private final GlobalSettingsService globalSettingsService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  private final NoteRealmService noteRealmService;
  private final NoteRepository noteRepository;
  private final ResolvedWikiLinkService resolvedWikiLinkService;
  private final AuthorizationService authorizationService;

  @Autowired
  public QuestionGenerationRequestBuilder(
      GlobalSettingsService globalSettingsService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      NoteRealmService noteRealmService,
      NoteRepository noteRepository,
      ResolvedWikiLinkService resolvedWikiLinkService,
      AuthorizationService authorizationService) {
    this.globalSettingsService = globalSettingsService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.noteRealmService = noteRealmService;
    this.noteRepository = noteRepository;
    this.resolvedWikiLinkService = resolvedWikiLinkService;
    this.authorizationService = authorizationService;
  }

  public StructuredResponseCreateParams<GeneratedMcq> buildQuestionGenerationResponseRequest(
      Note note, String additionalMessage, Long contextSeed) {
    return buildQuestionGenerationResponseRequest(note, additionalMessage, contextSeed, null);
  }

  @Transactional(readOnly = true)
  public StructuredResponseCreateParams<GeneratedMcq> buildQuestionGenerationResponseRequest(
      Note note, String additionalMessage, Long contextSeed, String propertyKey) {
    return buildQuestionGenerationResponseRequest(
        note, additionalMessage, contextSeed, propertyKey, authorizationService.getCurrentUser());
  }

  public StructuredResponseCreateParams<GeneratedMcq> buildQuestionGenerationResponseRequest(
      Note note, String additionalMessage, Long contextSeed, String propertyKey, User viewer) {
    return buildQuestionGenerationResponseRequestInternal(
        note, additionalMessage, contextSeed, propertyKey, viewer, false);
  }

  public StructuredResponseCreateParams<GeneratedMcq>
      buildQuestionGenerationResponseRequestForBatch(
          Note note, String additionalMessage, Long contextSeed, String propertyKey, User viewer) {
    return buildQuestionGenerationResponseRequestInternal(
        note, additionalMessage, contextSeed, propertyKey, viewer, true);
  }

  private StructuredResponseCreateParams<GeneratedMcq>
      buildQuestionGenerationResponseRequestInternal(
          Note note,
          String additionalMessage,
          Long contextSeed,
          String propertyKey,
          User viewer,
          boolean batch) {
    String modelName = globalSettingsService.globalSettingQuestionGeneration().getValue();
    ReasoningEffort reasoningEffort =
        OpenAiModelCapabilities.questionGenerationReasoningEffort(modelName, batch);

    InstructionAndSchema tool =
        AiToolFactory.mcqAiTool(hydrateFocusNoteForQuestionGeneration(note).isBodyContentBlank());
    OpenAIResponseRequestBuilder<GeneratedMcq> responseRequestBuilder =
        openAiResponseRequestForQuestionGeneration(
            GeneratedMcq.class, note, additionalMessage, contextSeed, propertyKey, viewer);
    responseRequestBuilder.addInstruction(tool.getMessageBody());
    responseRequestBuilder.reasoningEffort(reasoningEffort);
    responseRequestBuilder.maxOutputTokens(
        OpenAiModelCapabilities.questionGenerationMaxOutputTokens(reasoningEffort, batch));
    if (batch) {
      return responseRequestBuilder.buildForBatchApi();
    }
    return responseRequestBuilder.build();
  }

  private Note hydrateFocusNoteForQuestionGeneration(Note note) {
    return noteRepository
        .hydrateNonDeletedNotesWithNotebookAndFolderByIds(List.of(note.getId()))
        .stream()
        .findFirst()
        .orElse(note);
  }

  public <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionGeneration(
      Class<T> responseType, Note note, String additionalMessage, Long contextSeed) {
    return openAiResponseRequestForQuestionGeneration(
        responseType, note, additionalMessage, contextSeed, null);
  }

  public <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionGeneration(
      Class<T> responseType,
      Note note,
      String additionalMessage,
      Long contextSeed,
      String propertyKey) {
    return openAiResponseRequestForQuestionGeneration(
        responseType,
        note,
        additionalMessage,
        contextSeed,
        propertyKey,
        authorizationService.getCurrentUser());
  }

  public <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionGeneration(
      Class<T> responseType,
      Note note,
      String additionalMessage,
      Long contextSeed,
      String propertyKey,
      User viewer) {
    String modelName = globalSettingsService.globalSettingQuestionGeneration().getValue();
    return openAiResponseRequestForQuestionGeneration(
        responseType, note, additionalMessage, contextSeed, modelName, propertyKey, viewer);
  }

  public <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionEvaluation(
      Class<T> responseType, Note note, String additionalMessage, Long contextSeed) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    return openAiResponseRequestForQuestionGeneration(
        responseType,
        note,
        additionalMessage,
        contextSeed,
        modelName,
        null,
        authorizationService.getCurrentUser());
  }

  private <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionGeneration(
      Class<T> responseType,
      Note note,
      String additionalMessage,
      Long contextSeed,
      String modelName,
      String propertyKey,
      User viewer) {
    Note focus = hydrateFocusNoteForQuestionGeneration(note);

    List<String> instructionBlocks = noteRealmService.questionGenerationInstructionBlocks(focus);
    String instruction =
        instructionBlocks.isEmpty() ? null : String.join("\n\n", instructionBlocks);
    String instructionUserBlock =
        instruction != null ? CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER + "\n" + instruction : null;
    int instructionTokens =
        instructionUserBlock != null
            ? ApproximateUtf8TokenBudget.estimateApproxTokens(instructionUserBlock)
            : 0;
    String propertyFocusBlock =
        propertyKey != null && !propertyKey.isBlank()
            ? FocusContextMarkdownAugmenter.buildPropertyFocusBlock(focus, propertyKey)
            : null;
    int propertyFocusTokens =
        propertyFocusBlock != null
            ? ApproximateUtf8TokenBudget.estimateApproxTokens(propertyFocusBlock)
            : 0;
    int focusBudget =
        Math.max(
            0,
            FocusContextConstants.FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET
                - instructionTokens
                - propertyFocusTokens);
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(contextSeed, focusBudget);

    FocusContextResult focusContextResult =
        focusContextRetrievalService.retrieve(focus, viewer, config);
    String focusContextMarkdown = focusContextMarkdownRenderer.render(focusContextResult, config);
    if (propertyFocusBlock != null) {
      focusContextMarkdown =
          FocusContextMarkdownAugmenter.embedPropertyFocus(
              focusContextMarkdown, propertyFocusBlock);
      focusContextMarkdown =
          FocusContextMarkdownAugmenter.ensureWikiLinks(
              focusContextMarkdown, resolvedWikiLinkService.wikiLinksForViewer(focus, viewer));
    }

    OpenAIResponseRequestBuilder<T> builder =
        new OpenAIResponseRequestBuilder<T>(responseType).model(modelName);
    if (instructionUserBlock != null) {
      builder.addUserMessage(instructionUserBlock);
    }
    builder.addUserMessage(focusContextMarkdown);
    if (additionalMessage != null) {
      builder.addUserMessage(additionalMessage);
    }
    return builder;
  }
}
