package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiQuestionGenerator {
  private final NoteQuestionGenerationService noteQuestionGenerationService;
  private final GeneratedQuestionPostProcessor generatedQuestionPostProcessor;

  @Autowired
  public AiQuestionGenerator(
      NoteQuestionGenerationService noteQuestionGenerationService,
      GeneratedQuestionPostProcessor generatedQuestionPostProcessor) {
    this.noteQuestionGenerationService = noteQuestionGenerationService;
    this.generatedQuestionPostProcessor = generatedQuestionPostProcessor;
  }

  public GeneratedMcq getAiGeneratedQuestion(Note note, String additionalMessage) {
    return getAiGeneratedQuestion(note, additionalMessage, null, null);
  }

  public GeneratedMcq getAiGeneratedQuestion(
      Note note, String additionalMessage, Long contextSeed) {
    return getAiGeneratedQuestion(note, additionalMessage, contextSeed, null);
  }

  public GeneratedMcq getAiGeneratedQuestion(
      Note note, String additionalMessage, Long contextSeed, String propertyKey) {
    try {
      GeneratedMcq original =
          noteQuestionGenerationService.generateQuestion(
              note, additionalMessage, contextSeed, propertyKey);
      return generatedQuestionPostProcessor.postProcess(original);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public GeneratedMcq getAiGeneratedRefineQuestion(Note note, Mcq mcq) {
    return noteQuestionGenerationService
        .refineQuestion(note, mcq)
        .map(generatedQuestionPostProcessor::postProcess)
        .orElse(null);
  }

  public QuestionEvaluation getQuestionContestResult(Note note, Mcq mcq) {
    try {
      return noteQuestionGenerationService.evaluateQuestion(note, mcq).orElse(null);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public GeneratedMcq regenerateQuestion(
      QuestionContestResult contestResult, Note note, Mcq mcq, Long contextSeed) {
    return regenerateQuestion(contestResult, note, mcq, contextSeed, null);
  }

  public GeneratedMcq regenerateQuestion(
      QuestionContestResult contestResult,
      Note note,
      Mcq mcq,
      Long contextSeed,
      String propertyKey) {
    String additionalMessage = AiToolFactory.buildRegenerateQuestionMessage(contestResult, mcq);
    return getAiGeneratedQuestion(note, additionalMessage, contextSeed, propertyKey);
  }
}
