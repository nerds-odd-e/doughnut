package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.services.ai.AiQuestionGenerator;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class AiQuestionFactory extends PredefinedQuestionFactory {
  private final Note note;
  AiQuestionGenerator aiQuestionGenerator;

  public AiQuestionFactory(Note note, AiQuestionGenerator questionGenerator) {
    this.note = note;
    this.aiQuestionGenerator = questionGenerator;
  }

  @Override
  public PredefinedQuestion buildValidPredefinedQuestion()
      throws PredefinedQuestionNotPossibleException {
    MCQWithAnswer MCQWithAnswer = aiQuestionGenerator.getAiGeneratedQuestion(note);
    if (MCQWithAnswer == null) {
      throw new PredefinedQuestionNotPossibleException();
    }
    return PredefinedQuestion.fromMCQWithAnswer(MCQWithAnswer, note);
  }
}
