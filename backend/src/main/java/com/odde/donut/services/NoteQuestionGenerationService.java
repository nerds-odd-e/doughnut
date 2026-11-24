package com.odde.donut.services;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.entities.FocusNoteInput;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.PropertyFocus;
import com.odde.donut.services.QuestionGenerationRequestBuilder.PreparedQuestionGeneration;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.ai.SupportedChoices;
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

  public GeneratedMcq generateQuestion(Note note, String additionalMessage) {
    return generateQuestion(note, additionalMessage, null, null);
  }

  public GeneratedMcq generateQuestion(Note note, String additionalMessage, Long contextSeed) {
    return generateQuestion(note, additionalMessage, contextSeed, null);
  }

  public GeneratedMcq generateQuestion(
      Note note, String additionalMessage, Long contextSeed, PropertyFocus propertyFocus) {
    return generateQuestionFromRequest(
        requestBuilder.buildQuestionGenerationResponseRequest(
            note, additionalMessage, contextSeed, propertyFocus));
  }

  public GeneratedQuestion generateQuestionWithFocus(
      Note note, String additionalMessage, Long contextSeed, PropertyFocus propertyFocus) {
    PreparedQuestionGeneration prepared =
        requestBuilder.prepareQuestionGeneration(
            note, additionalMessage, contextSeed, propertyFocus);
    return new GeneratedQuestion(
        generateQuestionFromRequest(prepared.request()), prepared.focusNoteInput());
  }

  public record GeneratedQuestion(GeneratedMcq question, FocusNoteInput focusNoteInput) {}

  private GeneratedMcq generateQuestionFromRequest(
      StructuredResponseCreateParams<GeneratedMcq> responseRequest) {
    return openAiApiHandler
        .requestAndGetStructuredResponseResult(responseRequest)
        .flatMap(this::validQuestion)
        .orElse(null);
  }

  private Optional<GeneratedMcq> validQuestion(GeneratedMcq question) {
    if (question == null || !question.isValid()) {
      return Optional.empty();
    }
    return Optional.of(question);
  }

  public Optional<SupportedChoices> supportedChoices(Note note, Mcq question) {
    StructuredResponseCreateParams<SupportedChoices> responseRequest =
        requestBuilder
            .openAiResponseRequestForContestCheck(SupportedChoices.class, note)
            .addInstruction(SupportedChoices.INSTRUCTIONS)
            .addUserMessage(
                "Multiple-choice question:\n\n"
                    + ObjectMapperConfig.handWrittenJsonMapper()
                        .valueToTree(question.withoutSolution())
                        .toString())
            .reasoningEffort(ReasoningEffort.LOW)
            .maxOutputTokens(500L)
            .build();

    return openAiApiHandler.requestAndGetStructuredResponseResult(responseRequest);
  }
}
