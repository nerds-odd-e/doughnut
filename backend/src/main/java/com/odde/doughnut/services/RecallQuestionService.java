package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecallQuestionService {
  private final PredefinedQuestionService predefinedQuestionService;
  private final EntityPersister entityPersister;
  private final AiQuestionGenerator aiQuestionGenerator;
  private final MemoryTrackerService memoryTrackerService;

  @Autowired
  public RecallQuestionService(
      EntityPersister entityPersister,
      MemoryTrackerService memoryTrackerService,
      PredefinedQuestionService predefinedQuestionService,
      AiQuestionGenerator aiQuestionGenerator) {
    this.entityPersister = entityPersister;
    this.memoryTrackerService = memoryTrackerService;
    this.predefinedQuestionService = predefinedQuestionService;
    this.aiQuestionGenerator = aiQuestionGenerator;
  }

  public PredefinedQuestion generateAQuestion(MemoryTracker memoryTracker) {
    return predefinedQuestionService.generateAFeasibleQuestion(memoryTracker.getNote());
  }

  public PredefinedQuestion regenerateAQuestion(
      QuestionContestResult contestResult, Note note, MCQWithAnswer mcqWithAnswer) {
    MCQWithAnswer MCQWithAnswer =
        aiQuestionGenerator.regenerateQuestion(contestResult, note, mcqWithAnswer);
    if (MCQWithAnswer == null) {
      return null;
    }
    PredefinedQuestion question = PredefinedQuestion.fromMCQWithAnswer(MCQWithAnswer, note);
    return entityPersister.save(question);
  }

  private QuestionAnswer createQuestionAnswerFromQuestion(
      PredefinedQuestion question, AnswerDTO answerDTO) {
    QuestionAnswer questionAnswer = new QuestionAnswer();
    questionAnswer.setPredefinedQuestion(question);
    questionAnswer.buildAnswer(answerDTO);
    return entityPersister.save(questionAnswer);
  }

  public QuestionContestResult contest(PredefinedQuestion predefinedQuestion) {
    return predefinedQuestionService.contest(predefinedQuestion);
  }

  public AnsweredQuestion answerQuestion(
      PredefinedQuestion predefinedQuestion,
      AnswerDTO answerDTO,
      User user,
      Timestamp currentUTCTimestamp) {
    QuestionAnswer questionAnswer = createQuestionAnswerFromQuestion(predefinedQuestion, answerDTO);
    Answer answer = questionAnswer.getAnswer();
    memoryTrackerService.updateMemoryTrackerAfterAnsweringQuestion(
        user, currentUTCTimestamp, answer.getCorrect(), predefinedQuestion);
    return questionAnswer.getAnsweredQuestion();
  }
}
