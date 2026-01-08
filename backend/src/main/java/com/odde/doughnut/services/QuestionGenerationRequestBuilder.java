package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteType;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
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
    OpenAIChatRequestBuilder chatRequestBuilder = getChatRequestBuilder(note);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addToOverallSystemMessage(instructions);
    }

    // Include relation type specific instructions if the note is a relationship note
    // Include note type specific instructions if the note has a type (not null)
    RelationType relationType = note.isRelation() ? note.getRelationType() : null;
    NoteType noteType = note.getNoteType();
    chatRequestBuilder.responseJsonSchema(
        AiToolFactory.mcqWithAnswerAiTool(relationType, noteType));
    // Add any additional message if provided (after the question generation instruction)
    if (additionalMessage != null) {
      chatRequestBuilder.addUserMessage(additionalMessage);
    }
    return chatRequestBuilder.build();
  }

  public OpenAIChatRequestBuilder getChatRequestBuilder(Note note) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    String noteDescription = graphRAGService.getGraphRAGDescription(note);
    String noteInstructions =
        "The JSON below is available only to you (the question generator). The user who will later answer the question does NOT see this JSON. You must NEVER refer to it explicitly or implicitly. Do NOT use words like \"this note\", \"above\", \"the focus note\", or anything revealing that the question originates from hidden context.\n";

    return new OpenAIChatRequestBuilder()
        .model(modelName)
        .addUserMessage(noteInstructions + noteDescription)
        .addUserMessage(
            note.getIgnoredChecklistTopics() != null && !note.getIgnoredChecklistTopics().isEmpty()
                ? "Ignore the topic '" + note.getIgnoredChecklistTopics() + "'"
                : "");
  }
}
