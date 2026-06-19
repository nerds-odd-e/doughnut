package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteQuestionGenerationService {
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationRequestBuilder requestBuilder;

  @Autowired
  public NoteQuestionGenerationService(
      OpenAiApiHandler openAiApiHandler, QuestionGenerationRequestBuilder requestBuilder) {
    this.openAiApiHandler = openAiApiHandler;
    this.requestBuilder = requestBuilder;
  }

  public MCQWithAnswer generateQuestion(Note note, String additionalMessage)
      throws JsonProcessingException {
    return generateQuestion(note, additionalMessage, null, null);
  }

  public MCQWithAnswer generateQuestion(Note note, String additionalMessage, Long contextSeed)
      throws JsonProcessingException {
    return generateQuestion(note, additionalMessage, contextSeed, null);
  }

  public MCQWithAnswer generateQuestion(
      Note note, String additionalMessage, Long contextSeed, String propertyKey)
      throws JsonProcessingException {
    return generateQuestionWithResponses(note, additionalMessage, contextSeed, propertyKey);
  }

  public StructuredResponseCreateParams<MCQWithAnswer> buildQuestionGenerationRequest(
      Note note, String additionalMessage) {
    return buildQuestionGenerationRequest(note, additionalMessage, null);
  }

  public StructuredResponseCreateParams<MCQWithAnswer> buildQuestionGenerationRequest(
      Note note, String additionalMessage, String propertyKey) {
    return requestBuilder.buildQuestionGenerationResponseRequest(
        note, additionalMessage, null, propertyKey);
  }

  public StructuredResponseCreateParams<MCQWithAnswer> buildQuestionGenerationRequest(
      Note note, String additionalMessage, Long contextSeed, String propertyKey, User viewer) {
    return requestBuilder.buildQuestionGenerationResponseRequest(
        note, additionalMessage, contextSeed, propertyKey, viewer);
  }

  public <T> OpenAIResponseRequestBuilder<T> openAiResponseRequestForSharedNoteContext(
      Class<T> responseType, Note note, String additionalMessage) {
    return requestBuilder.openAiResponseRequestForQuestionGeneration(
        responseType, note, additionalMessage, null);
  }

  private MCQWithAnswer generateQuestionWithResponses(
      Note note, String additionalMessage, Long contextSeed, String propertyKey) {
    StructuredResponseCreateParams<MCQWithAnswer> responseRequest =
        requestBuilder.buildQuestionGenerationResponseRequest(
            note, additionalMessage, contextSeed, propertyKey);

    return openAiApiHandler
        .requestAndGetStructuredResponseResult(responseRequest)
        .map(question -> question != null && question.isValid() ? question : null)
        .orElse(null);
  }

  public Optional<QuestionEvaluation> evaluateQuestion(Note note, MCQWithAnswer question)
      throws JsonProcessingException {
    return evaluateQuestionWithResponses(note, question);
  }

  private Optional<QuestionEvaluation> evaluateQuestionWithResponses(
      Note note, MCQWithAnswer question) {
    InstructionAndSchema tool = AiToolFactory.questionEvaluationAiTool(question);
    var responseRequestBuilder =
        requestBuilder.openAiResponseRequestForQuestionEvaluation(
            QuestionEvaluation.class, note, null, null);
    responseRequestBuilder.addInstruction(tool.getMessageBody());
    if (note.getNotebookAssistantInstructions() != null
        && !note.getNotebookAssistantInstructions().isBlank()) {
      responseRequestBuilder.addInstruction(note.getNotebookAssistantInstructions());
    }
    StructuredResponseCreateParams<QuestionEvaluation> responseRequest =
        responseRequestBuilder.reasoningEffort(ReasoningEffort.LOW).maxOutputTokens(500L).build();

    return openAiApiHandler.requestAndGetStructuredResponseResult(responseRequest);
  }
}
