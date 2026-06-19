package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.ai.builder.OpenAIResponseRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiQuestionGenerator {
  private final NoteQuestionGenerationService noteQuestionGenerationService;
  private final GlobalSettingsService globalSettingsService;
  private final OpenAiApiHandler openAiApiHandler;
  private final GeneratedQuestionPostProcessor generatedQuestionPostProcessor;

  @Autowired
  public AiQuestionGenerator(
      NoteQuestionGenerationService noteQuestionGenerationService,
      GlobalSettingsService globalSettingsService,
      OpenAiApiHandler openAiApiHandler,
      GeneratedQuestionPostProcessor generatedQuestionPostProcessor) {
    this.noteQuestionGenerationService = noteQuestionGenerationService;
    this.globalSettingsService = globalSettingsService;
    this.openAiApiHandler = openAiApiHandler;
    this.generatedQuestionPostProcessor = generatedQuestionPostProcessor;
  }

  public MCQWithAnswer getAiGeneratedQuestion(Note note, String additionalMessage) {
    return getAiGeneratedQuestion(note, additionalMessage, null, null);
  }

  public MCQWithAnswer getAiGeneratedQuestion(
      Note note, String additionalMessage, Long contextSeed) {
    return getAiGeneratedQuestion(note, additionalMessage, contextSeed, null);
  }

  public MCQWithAnswer getAiGeneratedQuestion(
      Note note, String additionalMessage, Long contextSeed, String propertyKey) {
    try {
      MCQWithAnswer original =
          noteQuestionGenerationService.generateQuestion(
              note, additionalMessage, contextSeed, propertyKey);
      return generatedQuestionPostProcessor.postProcess(original);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public MCQWithAnswer getAiGeneratedRefineQuestion(Note note, MCQWithAnswer mcqWithAnswer) {
    return forNote(note, globalSettingsService.globalSettingQuestionGeneration().getValue())
        .refineQuestion(mcqWithAnswer)
        .orElse(null);
  }

  public QuestionEvaluation getQuestionContestResult(Note note, MCQWithAnswer mcqWithAnswer) {
    try {
      return noteQuestionGenerationService.evaluateQuestion(note, mcqWithAnswer).orElse(null);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private AiQuestionGeneratorForNote forNote(Note note, String modelName1) {
    OpenAIResponseRequestBuilder<MCQWithAnswerForRefinement> responseRequestBuilder =
        noteQuestionGenerationService.openAiResponseRequestForSharedNoteContext(
            MCQWithAnswerForRefinement.class, note, null);
    responseRequestBuilder.model(modelName1);
    return new AiQuestionGeneratorForNote(openAiApiHandler, responseRequestBuilder);
  }

  public MCQWithAnswer regenerateQuestion(
      QuestionContestResult contestResult,
      Note note,
      MCQWithAnswer mcqWithAnswer,
      Long contextSeed) {
    return regenerateQuestion(contestResult, note, mcqWithAnswer, contextSeed, null);
  }

  public MCQWithAnswer regenerateQuestion(
      QuestionContestResult contestResult,
      Note note,
      MCQWithAnswer mcqWithAnswer,
      Long contextSeed,
      String propertyKey) {
    String additionalMessage =
        AiToolFactory.buildRegenerateQuestionMessage(contestResult, mcqWithAnswer);
    return getAiGeneratedQuestion(note, additionalMessage, contextSeed, propertyKey);
  }
}
