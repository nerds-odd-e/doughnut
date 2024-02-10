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

    quizQuestionFactory.validatePossibility();

    QuizQuestionEntity quizQuestion;
    if (this == CLOZE_SELECTION) {
      quizQuestion = new QuizQuestionClozeSelection();
    } else if (this == SPELLING) {
      quizQuestion = new QuizQuestionSpelling();
    } else if (this == PICTURE_TITLE) {
      quizQuestion = new QuizQuestionPictureTitle();
    } else if (this == PICTURE_SELECTION) {
      quizQuestion = new QuizQuestionPictureSelection();
    } else if (this == LINK_TARGET) {
      quizQuestion = new QuizQuestionLinkTarget();
    } else if (this == LINK_SOURCE) {
      quizQuestion = new QuizQuestionLinkSource();
    } else if (this == LINK_SOURCE_WITHIN_SAME_LINK_TYPE) {
      quizQuestion = new QuizQuestionLinkSourceWithSameLinkType();
    } else if (this == CLOZE_LINK_TARGET) {
      quizQuestion = new QuizQuestionClozeLinkTarget();
    } else if (this == DESCRIPTION_LINK_TARGET) {
      quizQuestion = new QuizQuestionDescriptionLinkTarget();
    } else if (this == WHICH_SPEC_HAS_INSTANCE) {
      quizQuestion = new QuizQuestionWhichSpecHasInstance();
    } else if (this == FROM_SAME_PART_AS) {
      quizQuestion = new QuizQuestionFromSamePartAs();
    } else if (this == FROM_DIFFERENT_PART_AS) {
      quizQuestion = new QuizQuestionFromDifferentPartAs();
    } else if (this == AI_QUESTION) {
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
      quizQuestion.setChoicesAndRightAnswer(answerNote, options, servant1.randomizer);
    }

    if (quizQuestionFactory instanceof SecondaryReviewPointsFactory secondaryReviewPointsFactory) {
      quizQuestion.setCategoryLink(secondaryReviewPointsFactory.getCategoryLink());
    }
    return quizQuestion;
  }
}
