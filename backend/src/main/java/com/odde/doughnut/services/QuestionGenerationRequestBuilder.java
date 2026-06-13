package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.controllers.dto.WikiTitle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.focusContext.FocusContextConstants;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationRequestBuilder {
  static final String CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER = "Custom instruction for focus note:";
  static final String PROPERTY_FOCUS_CONTEXT_HEADER = "Focus on one property of the focus note:";

  private final GlobalSettingsService globalSettingsService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  private final NoteRealmService noteRealmService;
  private final NoteRepository noteRepository;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final AuthorizationService authorizationService;

  @Autowired
  public QuestionGenerationRequestBuilder(
      GlobalSettingsService globalSettingsService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      NoteRealmService noteRealmService,
      NoteRepository noteRepository,
      WikiTitleCacheService wikiTitleCacheService,
      AuthorizationService authorizationService) {
    this.globalSettingsService = globalSettingsService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.noteRealmService = noteRealmService;
    this.noteRepository = noteRepository;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.authorizationService = authorizationService;
  }

  public StructuredResponseCreateParams<MCQWithAnswer> buildQuestionGenerationResponseRequest(
      Note note, String additionalMessage, Long contextSeed) {
    return buildQuestionGenerationResponseRequest(note, additionalMessage, contextSeed, null);
  }

  public StructuredResponseCreateParams<MCQWithAnswer> buildQuestionGenerationResponseRequest(
      Note note, String additionalMessage, Long contextSeed, String propertyKey) {
    InstructionAndSchema tool =
        AiToolFactory.mcqWithAnswerAiTool(
            hydrateFocusNoteForQuestionGeneration(note).isBodyContentBlank());
    OpenAIResponseRequestBuilder<MCQWithAnswer> responseRequestBuilder =
        openAiResponseRequestForQuestionGeneration(
            MCQWithAnswer.class, note, additionalMessage, contextSeed, propertyKey);
    responseRequestBuilder.addInstruction(tool.getMessageBody());
    addNotebookAssistantInstructionsIfPresent(responseRequestBuilder, note);
    return responseRequestBuilder
        .reasoningEffort(ReasoningEffort.LOW)
        .maxOutputTokens(1000L)
        .build();
  }

  private static void addNotebookAssistantInstructionsIfPresent(
      OpenAIResponseRequestBuilder<?> responseRequestBuilder, Note note) {
    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      responseRequestBuilder.addInstruction(instructions);
    }
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
    String modelName = globalSettingsService.globalSettingQuestionGeneration().getValue();
    return openAiResponseRequestForQuestionGeneration(
        responseType, note, additionalMessage, contextSeed, modelName, propertyKey);
  }

  public <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionEvaluation(
      Class<T> responseType, Note note, String additionalMessage, Long contextSeed) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    return openAiResponseRequestForQuestionGeneration(
        responseType, note, additionalMessage, contextSeed, modelName, null);
  }

  private <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionGeneration(
      Class<T> responseType,
      Note note,
      String additionalMessage,
      Long contextSeed,
      String modelName,
      String propertyKey) {
    Note focus = hydrateFocusNoteForQuestionGeneration(note);

    String instruction =
        noteRealmService.resolveScopedQuestionGenerationInstruction(focus).orElse(null);
    String instructionUserBlock =
        instruction != null ? CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER + "\n" + instruction : null;
    int instructionTokens =
        instructionUserBlock != null
            ? ApproximateUtf8TokenBudget.estimateApproxTokens(instructionUserBlock)
            : 0;
    String propertyFocusBlock =
        propertyKey != null && !propertyKey.isBlank()
            ? buildPropertyFocusBlock(focus, propertyKey)
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

    FocusContextResult focusContextResult = focusContextRetrievalService.retrieve(focus, config);
    String focusContextMarkdown = focusContextMarkdownRenderer.render(focusContextResult, config);
    if (propertyFocusBlock != null) {
      focusContextMarkdown =
          embedPropertyFocusInFocusContext(focusContextMarkdown, propertyFocusBlock);
      User viewer = authorizationService.getCurrentUser();
      List<WikiTitle> wikiTitles = wikiTitleCacheService.wikiTitlesForViewer(focus, viewer);
      focusContextMarkdown = ensureWikiTitlesInFocusContext(focusContextMarkdown, wikiTitles);
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

  private static String embedPropertyFocusInFocusContext(
      String focusContextMarkdown, String propertyFocusBlock) {
    int focusNoteSection = focusContextMarkdown.indexOf("\n## Focus Note");
    if (focusNoteSection >= 0) {
      return focusContextMarkdown.substring(0, focusNoteSection)
          + "\n"
          + propertyFocusBlock
          + focusContextMarkdown.substring(focusNoteSection);
    }
    return focusContextMarkdown + "\n\n" + propertyFocusBlock;
  }

  private static String buildPropertyFocusBlock(Note focus, String propertyKey) {
    String propertyValue =
        NoteContentMarkdown.splitLeadingFrontmatter(focus.getContent())
            .flatMap(split -> split.frontmatter().getString(propertyKey))
            .orElse("");
    StringBuilder block = new StringBuilder();
    block.append(PROPERTY_FOCUS_CONTEXT_HEADER).append("\n");
    block
        .append("Focus on property \"")
        .append(propertyKey)
        .append("\" of the focus note (not the whole note).\n");
    block.append(
        "Infer what this property means from the property name, the focus note content, and"
            + " the listed link targets in the focus context.\n");
    block.append("Property key: ").append(propertyKey).append("\n");
    block.append("Property value: ").append(propertyValue).append("\n");
    return block.toString();
  }

  private static String ensureWikiTitlesInFocusContext(
      String focusContextMarkdown, List<WikiTitle> wikiTitles) {
    if (wikiTitles.isEmpty()) {
      return focusContextMarkdown;
    }
    List<WikiTitle> missing = new ArrayList<>();
    for (WikiTitle wikiTitle : wikiTitles) {
      String linkText = wikiTitle.getLinkText();
      if (linkText != null && !linkText.isBlank() && !focusContextMarkdown.contains(linkText)) {
        missing.add(wikiTitle);
      }
    }
    if (missing.isEmpty()) {
      return focusContextMarkdown;
    }
    StringBuilder extended = new StringBuilder(focusContextMarkdown);
    extended.append("\n\n## Link targets (focus note)\n\n");
    for (WikiTitle wikiTitle : missing) {
      extended.append("- ").append(wikiTitle.getLinkText());
      if (wikiTitle.getNoteId() != null) {
        extended.append(" (resolved note id: ").append(wikiTitle.getNoteId()).append(")");
      }
      extended.append("\n");
    }
    return extended.toString();
  }
}
