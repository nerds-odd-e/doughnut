package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.focusContext.RetrievalConfig;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationRequestBuilder {
  private final GlobalSettingsService globalSettingsService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  @Autowired
  public QuestionGenerationRequestBuilder(
      GlobalSettingsService globalSettingsService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer) {
    this.globalSettingsService = globalSettingsService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
  }

  public ChatCompletionCreateParams buildQuestionGenerationRequest(
      Note note, String additionalMessage) {
    InstructionAndSchema tool = AiToolFactory.mcqWithAnswerAiTool();
    return openAiChatRequestForQuestionGeneration(note, additionalMessage)
        .responseJsonSchema(tool)
        .build();
  }

  /**
   * Focus context user message, optional notebook assistant system hints, and optional extra user
   * message—in the same order as {@link #buildQuestionGenerationRequest}. The MCQ JSON schema
   * instruction is not attached here; {@link
   * com.odde.doughnut.services.openAiApis.OpenAiApiHandler#requestAndGetJsonSchemaResult} applies
   * it when calling the API.
   */
  public OpenAIChatRequestBuilder openAiChatRequestForQuestionGeneration(
      Note note, String additionalMessage) {
    OpenAIChatRequestBuilder chatRequestBuilder = getChatRequestBuilder(note);
    addNotebookAssistantInstructionsIfPresent(chatRequestBuilder, note);
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
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    RetrievalConfig config = RetrievalConfig.depth1();
    FocusContextResult focusContextResult = focusContextRetrievalService.retrieve(note, config);
    String focusContextMarkdown = focusContextMarkdownRenderer.render(focusContextResult, config);

    return new OpenAIChatRequestBuilder().model(modelName).addUserMessage(focusContextMarkdown);
  }
}
