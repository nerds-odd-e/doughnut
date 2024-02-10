package com.odde.doughnut.services;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import java.util.List;
import java.util.function.BiFunction;

public enum QuestionType {
  CLOZE_SELECTION(ClozeTitleSelectionQuizFactory::new, null),
  SPELLING(SpellingQuizFactory::new, null),
  PICTURE_TITLE(PictureTitleSelectionQuizFactory::new, null),
  PICTURE_SELECTION(PictureSelectionQuizFactory::new, null),
  LINK_TARGET(null, LinkTargetQuizFactory::new),
  LINK_SOURCE(null, LinkSourceQuizFactory::new),
  LINK_SOURCE_WITHIN_SAME_LINK_TYPE(null, LinkSourceWithinSameLinkTypeQuizFactory::new),
  CLOZE_LINK_TARGET(null, ClozeLinkTargetQuizFactory::new),
  DESCRIPTION_LINK_TARGET(null, DescriptionLinkTargetQuizFactory::new),
  WHICH_SPEC_HAS_INSTANCE(null, WhichSpecHasInstanceQuizFactory::new),
  FROM_SAME_PART_AS(null, FromSamePartAsQuizFactory::new),
  FROM_DIFFERENT_PART_AS(null, FromDifferentPartAsQuizFactory::new),
  AI_QUESTION(AiQuestionFactory::new, null);

  public final BiFunction<Note, QuizQuestionServant, QuizQuestionFactory> factory;
  public final BiFunction<LinkingNote, QuizQuestionServant, QuizQuestionFactory>
      factoryForLinkingNote;

  QuestionType(
      BiFunction<Note, QuizQuestionServant, QuizQuestionFactory> factory,
      BiFunction<LinkingNote, QuizQuestionServant, QuizQuestionFactory> factoryForLinkingNote) {
    this.factory = factory;
    this.factoryForLinkingNote = factoryForLinkingNote;
  }

  public QuizQuestionEntity buildQuizQuestion(Note note, QuizQuestionServant servant1)
      throws QuizQuestionNotPossibleException {
    QuizQuestionFactory quizQuestionFactory;
    if (note instanceof LinkingNote ln) {
      quizQuestionFactory = factoryForLinkingNote.apply(ln, servant1);
    } else {
      quizQuestionFactory = factory.apply(note, servant1);
    }

    QuizQuestionEntity quizQuestion = quizQuestionFactory.buildQuizQuestion();

    quizQuestionFactory.validatePossibility();
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
      quizQuestion.setChoicesAndRightAnswer(answerNote, options, servant1.randomizer);
    }

    if (quizQuestionFactory instanceof SecondaryReviewPointsFactory secondaryReviewPointsFactory) {
      quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
    }
    return quizQuestion;
  }
}
