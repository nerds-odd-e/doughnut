package com.odde.doughnut.services;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import java.util.function.Function;

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
  FROM_DIFFERENT_PART_AS(null, FromDifferentPartAsQuizFactory::new);

  public final Function<Note, QuizQuestionFactory> factory;
  public final Function<LinkingNote, QuizQuestionFactory> factoryForLinkingNote;

  QuestionType(
      Function<Note, QuizQuestionFactory> factory,
      Function<LinkingNote, QuizQuestionFactory> factoryForLinkingNote) {
    this.factory = factory;
    this.factoryForLinkingNote = factoryForLinkingNote;
  }

  public QuizQuestionFactory getQuizQuestionFactory(Note note1) {
    QuizQuestionFactory quizQuestionFactory;
    if (note1 instanceof LinkingNote ln) {
      quizQuestionFactory = factoryForLinkingNote.apply(ln);
    } else {
      quizQuestionFactory = factory.apply(note1);
    }
    return quizQuestionFactory;
  }
}
