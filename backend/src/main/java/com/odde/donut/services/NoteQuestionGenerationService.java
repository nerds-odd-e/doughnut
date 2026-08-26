package com.odde.donut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.ai.QuestionEvaluation;
import com.odde.donut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.donut.services.ai.tools.AiToolFactory;
import com.odde.donut.services.ai.tools.InstructionAndSchema;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
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

  public GeneratedMcq generateQuestion(Note note, String additionalMessage)
      throws JsonProcessingException {
    return generateQuestion(note, additionalMessage, null, null);
  }

  public GeneratedMcq generateQuestion(Note note, String additionalMessage, Long contextSeed)
      throws JsonProcessingException {
    return generateQuestion(note, additionalMessage, contextSeed, null);
  }

  public GeneratedMcq generateQuestion(
      Note note, String additionalMessage, Long contextSeed, String propertyKey)
      throws JsonProcessingException {
    return generateQuestionWithResponses(note, additionalMessage, contextSeed, propertyKey);
  }

  public StructuredResponseCreateParams<GeneratedMcq> buildQuestionGenerationRequest(
      Note note, String additionalMessage) {
    return buildQuestionGenerationRequest(note, additionalMessage, null);
  }

  public StructuredResponseCreateParams<GeneratedMcq> buildQuestionGenerationRequest(
      Note note, String additionalMessage, String propertyKey) {
    return requestBuilder.buildQuestionGenerationResponseRequest(
        note, additionalMessage, null, propertyKey);
  }

  public StructuredResponseCreateParams<GeneratedMcq> buildQuestionGenerationRequest(
      Note note, String additionalMessage, Long contextSeed, String propertyKey, User viewer) {
    return requestBuilder.buildQuestionGenerationResponseRequest(
        note, additionalMessage, contextSeed, propertyKey, viewer);
  }

  private GeneratedMcq generateQuestionWithResponses(
      Note note, String additionalMessage, Long contextSeed, String propertyKey) {
    StructuredResponseCreateParams<GeneratedMcq> responseRequest =
        requestBuilder.buildQuestionGenerationResponseRequest(
            note, additionalMessage, contextSeed, propertyKey);

    return openAiApiHandler
        .requestAndGetStructuredResponseResult(responseRequest)
        .flatMap(this::validQuestion)
        .orElse(null);
  }

  public Optional<GeneratedMcq> refineQuestion(Note note, Mcq question) {
    InstructionAndSchema tool = AiToolFactory.questionRefineAiTool(question);
    OpenAIResponseRequestBuilder<GeneratedMcq> responseRequestBuilder =
        requestBuilder.openAiResponseRequestForQuestionGeneration(
            GeneratedMcq.class, note, null, null);
    responseRequestBuilder.addInstruction(tool.getMessageBody());

    return openAiApiHandler
        .requestAndGetStructuredResponseResult(responseRequestBuilder.build())
        .flatMap(this::validQuestion);
  }

  private Optional<GeneratedMcq> validQuestion(GeneratedMcq question) {
    if (question == null || !question.isValid()) {
      return Optional.empty();
    }
    return Optional.of(question);
  }

  public Optional<QuestionEvaluation> evaluateQuestion(Note note, Mcq question)
      throws JsonProcessingException {
    return evaluateQuestionWithResponses(note, question);
  }

  private Optional<QuestionEvaluation> evaluateQuestionWithResponses(Note note, Mcq question) {
    InstructionAndSchema tool = AiToolFactory.questionEvaluationAiTool(question);
    var responseRequestBuilder =
        requestBuilder.openAiResponseRequestForQuestionEvaluation(
            QuestionEvaluation.class, note, null, null);
    responseRequestBuilder.addInstruction(tool.getMessageBody());
    StructuredResponseCreateParams<QuestionEvaluation> responseRequest =
        responseRequestBuilder.reasoningEffort(ReasoningEffort.LOW).maxOutputTokens(500L).build();

    return openAiApiHandler.requestAndGetStructuredResponseResult(responseRequest);
  }
}
