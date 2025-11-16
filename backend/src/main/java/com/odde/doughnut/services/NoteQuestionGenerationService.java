package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.util.Optional;

public class NoteQuestionGenerationService {
  protected final GlobalSettingsService globalSettingsService;
  private final Note note;
  private final OpenAiApiHandler openAiApiHandler;
  private final ObjectMapper objectMapper;

  public NoteQuestionGenerationService(
      GlobalSettingsService globalSettingsService,
      Note note,
      OpenAiApiHandler openAiApiHandler,
      ObjectMapper objectMapper) {
    this.globalSettingsService = globalSettingsService;
    this.note = note;
    this.openAiApiHandler = openAiApiHandler;
    this.objectMapper = objectMapper;
  }

  public MCQWithAnswer generateQuestion(MessageRequest additionalMessage)
      throws JsonProcessingException {
    return generateQuestionWithChatCompletion(additionalMessage);
  }

  public ChatCompletionRequest buildQuestionGenerationRequest(MessageRequest additionalMessage) {
    return buildQuestionGenerationRequest(
        globalSettingsService, note, objectMapper, additionalMessage);
  }

  public static ChatCompletionRequest buildQuestionGenerationRequest(
      GlobalSettingsService globalSettingsService,
      Note note,
      ObjectMapper objectMapper,
      MessageRequest additionalMessage) {
    OpenAIChatRequestBuilder chatRequestBuilder =
        getChatRequestBuilder(globalSettingsService, note, objectMapper);

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

  private MCQWithAnswer generateQuestionWithChatCompletion(MessageRequest additionalMessage) {
    OpenAIChatRequestBuilder chatRequestBuilder =
        getChatRequestBuilder(globalSettingsService, note, objectMapper);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addSystemMessage(instructions);
    }

    // Add any additional message if provided (before the question generation instruction)
    // Note: responseJsonSchema will add the question generation instruction
    if (additionalMessage != null) {
      chatRequestBuilder.addUserMessage(additionalMessage.getContent().toString());
    }

    chatRequestBuilder.responseJsonSchema(AiToolFactory.mcqWithAnswerAiTool());

    return openAiApiHandler
        .requestAndGetJsonSchemaResult(AiToolFactory.mcqWithAnswerAiTool(), chatRequestBuilder)
        .map(
            jsonNode -> {
              try {
                MCQWithAnswer question = objectMapper.treeToValue(jsonNode, MCQWithAnswer.class);

                // Validate the question
                if (question != null && question.isValid()) {
                  return question;
                }
                return null;
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .orElse(null);
  }

  public Optional<QuestionEvaluation> evaluateQuestion(MCQWithAnswer question)
      throws JsonProcessingException {
    return evaluateQuestionWithChatCompletion(question);
  }

  private Optional<QuestionEvaluation> evaluateQuestionWithChatCompletion(MCQWithAnswer question) {
    OpenAIChatRequestBuilder chatRequestBuilder =
        getChatRequestBuilder(globalSettingsService, note, objectMapper);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addSystemMessage(instructions);
    }

    Optional<JsonNode> result =
        openAiApiHandler.requestAndGetJsonSchemaResult(
            AiToolFactory.questionEvaluationAiTool(question), chatRequestBuilder);

    if (result.isEmpty()) {
      throw new RuntimeException("Failed to evaluate question: No valid response from API");
    }

    return result.map(
        jsonNode -> {
          try {
            return objectMapper.treeToValue(jsonNode, QuestionEvaluation.class);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private static OpenAIChatRequestBuilder getChatRequestBuilder(
      GlobalSettingsService globalSettingsService, Note note, ObjectMapper objectMapper) {
    String modelName = globalSettingsService.globalSettingEvaluation().getValue();
    String noteDescription = note.getGraphRAGDescription(objectMapper);
    return new OpenAIChatRequestBuilder().model(modelName).addSystemMessage(noteDescription);
  }
}
