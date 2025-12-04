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
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteQuestionGenerationService {
  protected final GlobalSettingsService globalSettingsService;
  private final OpenAiApiHandler openAiApiHandler;
  private final ObjectMapper objectMapper;
  private final QuestionGenerationRequestBuilder requestBuilder;

  @Autowired
  public NoteQuestionGenerationService(
      GlobalSettingsService globalSettingsService,
      OpenAiApiHandler openAiApiHandler,
      ObjectMapper objectMapper,
      QuestionGenerationRequestBuilder requestBuilder) {
    this.globalSettingsService = globalSettingsService;
    this.openAiApiHandler = openAiApiHandler;
    this.objectMapper = objectMapper;
    this.requestBuilder = requestBuilder;
  }

  public MCQWithAnswer generateQuestion(Note note, String additionalMessage)
      throws JsonProcessingException {
    return generateQuestionWithChatCompletion(note, additionalMessage);
  }

  public ChatCompletionCreateParams buildQuestionGenerationRequest(
      Note note, String additionalMessage) {
    return requestBuilder.buildQuestionGenerationRequest(note, additionalMessage);
  }

  private MCQWithAnswer generateQuestionWithChatCompletion(Note note, String additionalMessage) {
    OpenAIChatRequestBuilder chatRequestBuilder = requestBuilder.getChatRequestBuilder(note);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addToOverallSystemMessage(instructions);
    }

    // Add any additional message if provided (before the question generation instruction)
    // Note: responseJsonSchema will add the question generation instruction
    if (additionalMessage != null) {
      chatRequestBuilder.addUserMessage(additionalMessage);
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

  public Optional<QuestionEvaluation> evaluateQuestion(Note note, MCQWithAnswer question)
      throws JsonProcessingException {
    return evaluateQuestionWithChatCompletion(note, question);
  }

  private Optional<QuestionEvaluation> evaluateQuestionWithChatCompletion(
      Note note, MCQWithAnswer question) {
    OpenAIChatRequestBuilder chatRequestBuilder = requestBuilder.getChatRequestBuilder(note);

    String instructions = note.getNotebookAssistantInstructions();
    if (instructions != null && !instructions.trim().isEmpty()) {
      chatRequestBuilder.addToOverallSystemMessage(instructions);
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
}
