package com.odde.donut.services;

import com.odde.donut.algorithms.PropertyKeyNaming;
import com.odde.donut.algorithms.RelationshipNoteComposition;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.entities.User;
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
  private final NoteReferenceService noteReferenceService;
  private final AuthorizationService authorizationService;

  @Autowired
  public QuestionGenerationRequestBuilder(
      GlobalSettingsService globalSettingsService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      NoteRealmService noteRealmService,
      NoteReferenceService noteReferenceService,
      AuthorizationService authorizationService) {
    this.globalSettingsService = globalSettingsService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.noteRealmService = noteRealmService;
    this.noteReferenceService = noteReferenceService;
    this.authorizationService = authorizationService;
  }

  @Transactional(readOnly = true)
  public StructuredResponseCreateParams<GeneratedMcq> buildQuestionGenerationResponseRequest(
      Note note, String additionalMessage, Long contextSeed, PropertyFocus propertyFocus) {
    return buildQuestionGenerationResponseRequest(
        note, additionalMessage, contextSeed, propertyFocus, authorizationService.getCurrentUser());
  }

  public StructuredResponseCreateParams<GeneratedMcq> buildQuestionGenerationResponseRequest(
      Note note,
      String additionalMessage,
      Long contextSeed,
      PropertyFocus propertyFocus,
      User viewer) {
    return prepareQuestionGeneration(note, additionalMessage, contextSeed, propertyFocus, viewer)
        .request();
  }

  @Transactional(readOnly = true)
  public PreparedQuestionGeneration prepareQuestionGeneration(
      Note note, String additionalMessage, Long contextSeed, PropertyFocus propertyFocus) {
    return prepareQuestionGeneration(
        note, additionalMessage, contextSeed, propertyFocus, authorizationService.getCurrentUser());
  }

  private PreparedQuestionGeneration prepareQuestionGeneration(
      Note note,
      String additionalMessage,
      Long contextSeed,
      PropertyFocus propertyFocus,
      User viewer) {
    Note focus = focusContextRetrievalService.hydrateFocus(note);
    return new PreparedQuestionGeneration(
        buildMcqRequest(focus, additionalMessage, contextSeed, propertyFocus, viewer, false),
        FocusNoteFingerprint.of(focus));
  }

  public record PreparedQuestionGeneration(
      StructuredResponseCreateParams<GeneratedMcq> request, String focusNoteFingerprint) {}

  public StructuredResponseCreateParams<GeneratedMcq>
      buildQuestionGenerationResponseRequestForBatch(
          Note note,
          String additionalMessage,
          Long contextSeed,
          PropertyFocus propertyFocus,
          User viewer) {
    return buildMcqRequest(
        focusContextRetrievalService.hydrateFocus(note),
        additionalMessage,
        contextSeed,
        propertyFocus,
        viewer,
        true);
  }

  private StructuredResponseCreateParams<GeneratedMcq> buildMcqRequest(
      Note focus,
      String additionalMessage,
      Long contextSeed,
      PropertyFocus propertyFocus,
      User viewer,
      boolean batch) {
    String modelName = globalSettingsService.globalSettingQuestionGeneration().getValue();
    ReasoningEffort reasoningEffort =
        OpenAiModelCapabilities.questionGenerationReasoningEffort(modelName, batch);

    InstructionAndSchema tool = AiToolFactory.mcqAiTool(focus.isBodyContentBlank());
    OpenAIResponseRequestBuilder<GeneratedMcq> responseRequestBuilder =
        openAiResponseRequestForHydratedFocus(
            GeneratedMcq.class,
            focus,
            additionalMessage,
            contextSeed,
            modelName,
            propertyFocus,
            viewer);
    responseRequestBuilder.addInstruction(tool.getMessageBody());
    responseRequestBuilder.reasoningEffort(reasoningEffort);
    responseRequestBuilder.maxOutputTokens(
        OpenAiModelCapabilities.questionGenerationMaxOutputTokens(reasoningEffort, batch));
    if (batch) {
      return responseRequestBuilder.buildForBatchApi();
    }
    return responseRequestBuilder.build();
  }

  public <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForContestCheck(
      Class<T> responseType, Note note) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    return openAiResponseRequestForHydratedFocus(
        responseType,
        focusContextRetrievalService.hydrateFocus(note),
        null,
        null,
        modelName,
        null,
        authorizationService.getCurrentUser());
  }

  /**
   * The property type the question is about: the focused property's, otherwise the relation's when
   * the focus is a relationship note; null when neither.
   */
  private static String questionPropertyType(Note focus, PropertyFocus propertyFocus) {
    if (propertyFocus != null) {
      return PropertyKeyNaming.propertyType(propertyFocus.key());
    }
    return RelationshipNoteComposition.parse(focus.getContent())
        .map(RelationshipNoteComposition.Relationship::propertyKey)
        .map(PropertyKeyNaming::propertyType)
        .orElse(null);
  }

  private <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForHydratedFocus(
      Class<T> responseType,
      Note focus,
      String additionalMessage,
      Long contextSeed,
      String modelName,
      PropertyFocus propertyFocus,
      User viewer) {
    List<String> instructionBlocks =
        noteRealmService.questionGenerationInstructionBlocks(
            focus, questionPropertyType(focus, propertyFocus));
    String instruction =
        instructionBlocks.isEmpty() ? null : String.join("\n\n", instructionBlocks);
    String instructionUserBlock =
        instruction != null ? CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER + "\n" + instruction : null;
    int instructionTokens =
        instructionUserBlock != null
            ? ApproximateUtf8TokenBudget.estimateApproxTokens(instructionUserBlock)
            : 0;
    String propertyFocusBlock =
        propertyFocus != null
            ? FocusContextMarkdownAugmenter.buildPropertyFocusBlock(focus, propertyFocus)
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
        focusContextRetrievalService.retrieveFromHydratedFocus(focus, viewer, config);
    String focusContextMarkdown = focusContextMarkdownRenderer.render(focusContextResult, config);
    if (propertyFocusBlock != null) {
      focusContextMarkdown =
          FocusContextMarkdownAugmenter.embedPropertyFocus(
              focusContextMarkdown, propertyFocusBlock);
      focusContextMarkdown =
          FocusContextMarkdownAugmenter.ensureWikiLinks(
              focusContextMarkdown, noteReferenceService.wikiLinksForViewer(focus, viewer));
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
