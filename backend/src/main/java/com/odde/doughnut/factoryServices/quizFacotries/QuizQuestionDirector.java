package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.*;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionOptionsFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.QuestionRawJsonFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.SecondaryReviewPointsFactory;
import com.odde.doughnut.services.QuestionType;
import java.util.List;

public record QuizQuestionDirector(QuestionType questionType, QuizQuestionServant servant) {

  public QuizQuestionEntity invoke(Note note) throws QuizQuestionNotPossibleException {
    QuizQuestionFactory quizQuestionFactory;
    quizQuestionFactory = questionType.getQuizQuestionFactory(note, servant);

    quizQuestionFactory.validatePossibility();

    QuizQuestionEntity quizQuestion;
    if (questionType == QuestionType.CLOZE_SELECTION) {
      quizQuestion = new QuizQuestionClozeSelection();
    } else if (questionType == QuestionType.SPELLING) {
      quizQuestion = new QuizQuestionSpelling();
    } else if (questionType == QuestionType.PICTURE_TITLE) {
      quizQuestion = new QuizQuestionPictureTitle();
    } else if (questionType == QuestionType.PICTURE_SELECTION) {
      quizQuestion = new QuizQuestionPictureSelection();
    } else if (questionType == QuestionType.LINK_TARGET) {
      quizQuestion = new QuizQuestionLinkTarget();
    } else if (questionType == QuestionType.LINK_SOURCE) {
      quizQuestion = new QuizQuestionLinkSource();
    } else if (questionType == QuestionType.LINK_SOURCE_WITHIN_SAME_LINK_TYPE) {
      quizQuestion = new QuizQuestionLinkSourceWithSameLinkType();
    } else if (questionType == QuestionType.CLOZE_LINK_TARGET) {
      quizQuestion = new QuizQuestionClozeLinkTarget();
    } else if (questionType == QuestionType.DESCRIPTION_LINK_TARGET) {
      quizQuestion = new QuizQuestionDescriptionLinkTarget();
    } else if (questionType == QuestionType.WHICH_SPEC_HAS_INSTANCE) {
      quizQuestion = new QuizQuestionWhichSpecHasInstance();
    } else if (questionType == QuestionType.FROM_SAME_PART_AS) {
      quizQuestion = new QuizQuestionFromSamePartAs();
    } else if (questionType == QuestionType.FROM_DIFFERENT_PART_AS) {
      quizQuestion = new QuizQuestionFromDifferentPartAs();
    } else if (questionType == QuestionType.AI_QUESTION) {
      quizQuestion = new QuizQuestionAIQuestion();
    } else {
      throw new RuntimeException("Unknown question type");
    }
    quizQuestion.setNote(note);

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
