package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteType;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationRequestBuilder {
  private final GlobalSettingsService globalSettingsService;
  private final GraphRAGService graphRAGService;

  @Autowired
  public QuestionGenerationRequestBuilder(
      GlobalSettingsService globalSettingsService, GraphRAGService graphRAGService) {
    this.globalSettingsService = globalSettingsService;
    this.graphRAGService = graphRAGService;
  }

  public ChatCompletionCreateParams buildQuestionGenerationRequest(
      Note note, String additionalMessage) {
    RelationType relationType = note.isRelation() ? note.getRelationType() : null;
    NoteType noteType = note.getNoteType();
    InstructionAndSchema tool = AiToolFactory.mcqWithAnswerAiTool(relationType, noteType);
    return openAiChatRequestForQuestionGeneration(note, additionalMessage)
        .responseJsonSchema(tool)
        .build();
  }

  /**
   * Graph-RAG user message, optional notebook assistant system hints, and optional extra user
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
    String noteDescription = graphRAGService.getGraphRAGDescription(note);
    String noteInstructions =
        "You are the Question Designer for this request, producing a multiple-choice question (MCQ) from hidden context. The JSON below is visible only to you—not to the user who will answer. You must NEVER refer to it explicitly or implicitly. Do NOT use words like \"this note\", \"above\", \"the focus note\", or anything revealing that the question or choices originate from hidden context.\n";

    return new OpenAIChatRequestBuilder()
        .model(modelName)
        .addUserMessage(noteInstructions + noteDescription);
  }
}
