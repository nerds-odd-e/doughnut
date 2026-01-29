package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteType;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.List;
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

    var builder =
        new OpenAIChatRequestBuilder()
            .model(modelName)
            .addUserMessage(noteInstructions + noteDescription);

    // Add instruction to exclude ignored points from question generation
    String ignoredPointsInstruction = buildIgnoredPointsInstruction(note);
    if (ignoredPointsInstruction != null && !ignoredPointsInstruction.trim().isEmpty()) {
      builder.addToOverallSystemMessage(ignoredPointsInstruction);
    }

    return builder;
  }

  private String buildIgnoredPointsInstruction(Note note) {
    String ignoredPointsJson = note.getIgnoredPoints();
    if (ignoredPointsJson == null || ignoredPointsJson.trim().isEmpty()) {
      return null;
    }

    try {
      List<String> ignoredPoints =
          new ObjectMapperConfig()
              .objectMapper()
              .readValue(ignoredPointsJson, new TypeReference<List<String>>() {});

      if (ignoredPoints == null || ignoredPoints.isEmpty()) {
        return null;
      }

      StringBuilder instruction = new StringBuilder();
      instruction.append(
          "IMPORTANT: The following points from the note should NOT be used to generate questions. Do NOT create questions based on these points:\n\n");
      for (String point : ignoredPoints) {
        if (point != null && !point.trim().isEmpty()) {
          instruction.append("- ").append(point.trim()).append("\n");
        }
      }
      instruction.append(
          "\nWhen generating questions, completely avoid referencing or testing knowledge about these excluded points.");

      return instruction.toString();
    } catch (JsonProcessingException e) {
      // If JSON parsing fails, return null (don't add instruction)
      return null;
    }
  }
}
