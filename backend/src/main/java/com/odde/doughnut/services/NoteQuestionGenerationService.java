package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteQuestionGenerationService {
  private final OpenAiApiHandler openAiApiHandler;
  private final ObjectMapper objectMapper;
  private final QuestionGenerationRequestBuilder requestBuilder;

  @Autowired
  public NoteQuestionGenerationService(
      OpenAiApiHandler openAiApiHandler,
      ObjectMapper objectMapper,
      QuestionGenerationRequestBuilder requestBuilder) {
    this.openAiApiHandler = openAiApiHandler;
    this.objectMapper = objectMapper;
    this.requestBuilder = requestBuilder;
  }

  public MCQWithAnswer generateQuestion(Note note, String additionalMessage)
      throws JsonProcessingException {
    return generateQuestionWithCustomPrompt(note, null, additionalMessage);
  }

  public MCQWithAnswer generateQuestionWithCustomPrompt(
      Note note, String customPrompt, String additionalMessage) throws JsonProcessingException {
    return generateQuestionWithChatCompletion(note, customPrompt, additionalMessage);
  }

  public ChatCompletionCreateParams buildQuestionGenerationRequest(
      Note note, String additionalMessage) {
    return requestBuilder.buildQuestionGenerationRequest(note, additionalMessage);
  }

  /** Same message layout as MCQ generation / evaluation (GraphRAG context, notebook hints). */
  public OpenAIChatRequestBuilder openAiChatRequestForSharedNoteContext(
      Note note, String additionalMessage) {
    return requestBuilder.openAiChatRequestForQuestionGeneration(note, additionalMessage);
  }

  private MCQWithAnswer generateQuestionWithChatCompletion(
      Note note, String customPrompt, String additionalMessage) {
    String prompt = (customPrompt != null) ? customPrompt : AiToolFactory.getDefaultMcqPrompt();
    InstructionAndSchema tool = AiToolFactory.questionAiTool(prompt);
    OpenAIChatRequestBuilder chatRequestBuilder =
        requestBuilder.openAiChatRequestForQuestionGeneration(note, additionalMessage);

    return openAiApiHandler
        .requestAndGetJsonSchemaResult(tool, chatRequestBuilder)
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
    OpenAIChatRequestBuilder chatRequestBuilder =
        requestBuilder.openAiChatRequestForQuestionGeneration(note, null);

    Optional<JsonNode> result =
        openAiApiHandler.requestAndGetJsonSchemaResult(
            AiToolFactory.questionEvaluationAiTool(question), chatRequestBuilder);

    if (result.isEmpty()) {
      return Optional.empty();
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
