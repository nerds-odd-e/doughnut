package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
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
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationRequestBuilder {
  static final String CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER = "Custom instruction for focus note:";

  private final GlobalSettingsService globalSettingsService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  private final NoteRealmService noteRealmService;
  private final NoteRepository noteRepository;

  @Autowired
  public QuestionGenerationRequestBuilder(
      GlobalSettingsService globalSettingsService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer,
      NoteRealmService noteRealmService,
      NoteRepository noteRepository) {
    this.globalSettingsService = globalSettingsService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
    this.noteRealmService = noteRealmService;
    this.noteRepository = noteRepository;
  }

  public StructuredResponseCreateParams<MCQWithAnswer> buildQuestionGenerationResponseRequest(
      Note note, String additionalMessage, Long contextSeed) {
    InstructionAndSchema tool =
        AiToolFactory.mcqWithAnswerAiTool(
            hydrateFocusNoteForQuestionGeneration(note).isBodyContentBlank());
    OpenAIResponseRequestBuilder<MCQWithAnswer> responseRequestBuilder =
        openAiResponseRequestForQuestionGeneration(
            MCQWithAnswer.class, note, additionalMessage, contextSeed);
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
    String modelName = globalSettingsService.globalSettingQuestionGeneration().getValue();
    return openAiResponseRequestForQuestionGeneration(
        responseType, note, additionalMessage, contextSeed, modelName);
  }

  public <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionEvaluation(
      Class<T> responseType, Note note, String additionalMessage, Long contextSeed) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    return openAiResponseRequestForQuestionGeneration(
        responseType, note, additionalMessage, contextSeed, modelName);
  }

  private <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForQuestionGeneration(
      Class<T> responseType,
      Note note,
      String additionalMessage,
      Long contextSeed,
      String modelName) {
    Note focus = hydrateFocusNoteForQuestionGeneration(note);

    String instruction =
        noteRealmService.resolveScopedQuestionGenerationInstruction(focus).orElse(null);
    String instructionUserBlock =
        instruction != null ? CUSTOM_INSTRUCTION_USER_MESSAGE_HEADER + "\n" + instruction : null;
    int instructionTokens =
        instructionUserBlock != null
            ? ApproximateUtf8TokenBudget.estimateApproxTokens(instructionUserBlock)
            : 0;
    int focusBudget =
        Math.max(
            0,
            FocusContextConstants.FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET - instructionTokens);
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(contextSeed, focusBudget);

    FocusContextResult focusContextResult = focusContextRetrievalService.retrieve(focus, config);
    String focusContextMarkdown = focusContextMarkdownRenderer.render(focusContextResult, config);

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
