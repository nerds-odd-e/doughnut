package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionOptionsFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionRawJsonFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SecondaryReviewPointsFactory;
import java.util.List;

public record QuizQuestionDirector(
    QuizQuestionEntity.QuestionType questionType, QuizQuestionServant servant) {

  public QuizQuestionEntity invoke(Note note) throws QuizQuestionNotPossibleException {
    QuizQuestionFactory quizQuestionFactory;
    quizQuestionFactory = questionType.getQuizQuestionFactory(note, servant);

    quizQuestionFactory.validatePossibility();

    QuizQuestionEntity quizQuestion = new QuizQuestionEntity();
    quizQuestion.setNote(note);
    quizQuestion.setQuestionType(questionType);

    if (quizQuestionFactory instanceof QuestionRawJsonFactory rawJsonFactory) {
      rawJsonFactory.generateRawJsonQuestion(quizQuestion);
    }

    if (quizQuestionFactory instanceof QuestionOptionsFactory optionsFactory) {
      Note answerNote = optionsFactory.generateAnswer();
      if (answerNote == null) {
        throw new QuizQuestionNotPossibleException();
      }
      List<? extends Note> options = optionsFactory.generateFillingOptions();
      if (options.size() < optionsFactory.minimumOptionCount() - 1) {
        throw new QuizQuestionNotPossibleException();
      }
      quizQuestion.setChoicesAndRightAnswer(answerNote, options, servant.randomizer);
    }

    if (quizQuestionFactory instanceof SecondaryReviewPointsFactory secondaryReviewPointsFactory) {
      quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
    }
    return quizQuestion;
  }
}
