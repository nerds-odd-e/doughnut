package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class AiQuestionFactory extends PredefinedQuestionFactory {
  private final Note note;
  AiQuestionGenerator aiQuestionGenerator;
  private final PredefinedQuestion oldQuestion;
  private final QuestionContestResult contestResult;

  public AiQuestionFactory(Note note, AiQuestionGenerator questionGenerator) {
    this(note, questionGenerator, null, null);
  }

  public AiQuestionFactory(
      Note note,
      AiQuestionGenerator questionGenerator,
      PredefinedQuestion oldQuestion,
      QuestionContestResult contestResult) {
    this.note = note;
    this.aiQuestionGenerator = questionGenerator;
    this.oldQuestion = oldQuestion;
    this.contestResult = contestResult;
  }

  @Override
  public PredefinedQuestion buildValidPredefinedQuestion()
      throws PredefinedQuestionNotPossibleException {
    MCQWithAnswer MCQWithAnswer =
        aiQuestionGenerator.getAiGeneratedQuestion(note, oldQuestion, contestResult);
    if (MCQWithAnswer == null) {
      throw new PredefinedQuestionNotPossibleException();
    }
    return PredefinedQuestion.fromMCQWithAnswer(MCQWithAnswer, note);
  }
}
