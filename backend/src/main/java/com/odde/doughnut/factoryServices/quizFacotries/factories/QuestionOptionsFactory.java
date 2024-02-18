package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public abstract class QuestionOptionsFactory implements QuizQuestionFactory {
  @Override
  public QuizQuestionEntity buildQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    QuizQuestionWithNoteChoices quizQuestion = this.buildQuizQuestionObj(servant);
    this.validateBasicPossibility();
    Note answerNote = this.generateAnswer(servant);
    if (answerNote == null) {
      throw new QuizQuestionNotPossibleException();
    }
    List<? extends Note> options = this.generateFillingOptions(servant);
    if (options.size() < this.minimumOptionCount() - 1) {
      throw new QuizQuestionNotPossibleException();
    }
    quizQuestion.setChoicesAndRightAnswer(answerNote, options, servant.randomizer);
    return quizQuestion;
  }

  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {}

  public abstract QuizQuestionWithNoteChoices buildQuizQuestionObj(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException;

  public abstract Note generateAnswer(QuizQuestionServant servant);

  public abstract List<? extends Note> generateFillingOptions(QuizQuestionServant servant);

  public int minimumOptionCount() {
    return 2;
  }
  ;
}
