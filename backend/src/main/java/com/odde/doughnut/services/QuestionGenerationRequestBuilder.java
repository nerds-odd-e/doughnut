package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.theokanning.openai.assistants.message.MessageRequest;

public class QuestionGenerationRequestBuilder {
  private final GlobalSettingsService globalSettingsService;
  private final ObjectMapper objectMapper;

  public QuestionGenerationRequestBuilder(
      GlobalSettingsService globalSettingsService, ObjectMapper objectMapper) {
    this.globalSettingsService = globalSettingsService;
    this.objectMapper = objectMapper;
  }

  public ChatCompletionCreateParams buildQuestionGenerationRequest(
      Note note, MessageRequest additionalMessage) {
    OpenAIChatRequestBuilder chatRequestBuilder = getChatRequestBuilder(note);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addSystemMessage(instructions);
    }

    // Add the question generation instruction (this also sets up JSON schema response format)
    chatRequestBuilder.responseJsonSchema(AiToolFactory.mcqWithAnswerAiTool());
    // Add any additional message if provided (after the question generation instruction)
    if (additionalMessage != null) {
      chatRequestBuilder.addUserMessage(additionalMessage.getContent().toString());
    }
    return chatRequestBuilder.build();
  }

  public OpenAIChatRequestBuilder getChatRequestBuilder(Note note) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    String noteDescription = note.getGraphRAGDescription(objectMapper);
    return new OpenAIChatRequestBuilder().model(modelName).addSystemMessage(noteDescription);
  }
}
