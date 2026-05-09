package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.focusContext.FocusContextConstants;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import com.openai.models.ReasoningEffort;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationRequestBuilder {
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

  public ChatCompletionCreateParams buildQuestionGenerationRequest(
      Note note, String additionalMessage) {
    return buildQuestionGenerationRequest(note, additionalMessage, null);
  }

  public ChatCompletionCreateParams buildQuestionGenerationRequest(
      Note note, String additionalMessage, Long contextSeed) {
    InstructionAndSchema tool = AiToolFactory.mcqWithAnswerAiTool();
    OpenAIChatRequestBuilder chatRequestBuilder =
        openAiChatRequestForQuestionGeneration(note, additionalMessage, contextSeed);
    chatRequestBuilder.responseJsonSchema(tool);
    addNotebookAssistantInstructionsIfPresent(chatRequestBuilder, note);
    return chatRequestBuilder
        .reasoningEffort(ReasoningEffort.LOW)
        .maxCompletionTokens(1000L)
        .build();
  }

  /**
   * Optional scoped {@code question_generation_instruction} as a user message, then focus context
   * as a user message, then optional {@code additionalMessage}—same order as {@link
   * #buildQuestionGenerationRequest}. Notebook assistant hints and the MCQ JSON schema instruction
   * are not attached here; they are added after the schema instruction in {@link
   * #buildQuestionGenerationRequest} or when calling {@code OpenAiApiHandler}'s three-argument
   * {@code requestAndGetJsonSchemaResult}.
   */
  public OpenAIChatRequestBuilder openAiChatRequestForQuestionGeneration(
      Note note, String additionalMessage) {
    return openAiChatRequestForQuestionGeneration(note, additionalMessage, null);
  }

  public OpenAIChatRequestBuilder openAiChatRequestForQuestionGeneration(
      Note note, String additionalMessage, Long contextSeed) {
    OpenAIChatRequestBuilder chatRequestBuilder = getChatRequestBuilder(note, contextSeed);
    if (additionalMessage != null) {
      chatRequestBuilder.addUserMessage(additionalMessage);
    }
    return chatRequestBuilder;
  }

  private static void addNotebookAssistantInstructionsIfPresent(
      OpenAIChatRequestBuilder chatRequestBuilder, Note note) {
    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addToOverallSystemMessage(instructions);
    }
  }

  public OpenAIChatRequestBuilder getChatRequestBuilder(Note note) {
    return getChatRequestBuilder(note, null);
  }

  public OpenAIChatRequestBuilder getChatRequestBuilder(Note note, Long contextSeed) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    Note focus =
        noteRepository
            .hydrateNonDeletedNotesWithNotebookAndFolderByIds(List.of(note.getId()))
            .stream()
            .findFirst()
            .orElse(note);

    String instruction =
        noteRealmService.resolveScopedQuestionGenerationInstruction(focus).orElse(null);
    int instructionTokens =
        instruction != null ? ApproximateUtf8TokenBudget.estimateApproxTokens(instruction) : 0;
    int focusBudget =
        Math.max(
            0,
            FocusContextConstants.FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET - instructionTokens);
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(contextSeed, focusBudget);

    FocusContextResult focusContextResult = focusContextRetrievalService.retrieve(focus, config);
    String focusContextMarkdown = focusContextMarkdownRenderer.render(focusContextResult, config);

    OpenAIChatRequestBuilder builder = new OpenAIChatRequestBuilder().model(modelName);
    if (instruction != null) {
      builder.addUserMessage(instruction);
    }
    return builder.addUserMessage(focusContextMarkdown);
  }
}
