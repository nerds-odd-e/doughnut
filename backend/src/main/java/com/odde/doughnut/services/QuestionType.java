package com.odde.doughnut.services;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import java.util.function.Function;

public enum QuestionType {
  CLOZE_SELECTION(ClozeTitleSelectionQuizFactory::new, null),
  SPELLING(note -> new SpellingQuizFactory(note), null),
  PICTURE_TITLE(PictureTitleSelectionQuizFactory::new, null),
  PICTURE_SELECTION(note -> new PictureSelectionQuizFactory(note), null),
  LINK_TARGET(null, note -> new LinkTargetQuizFactory(note)),
  LINK_SOURCE(null, link -> new LinkSourceQuizFactory(link)),
  LINK_SOURCE_WITHIN_SAME_LINK_TYPE(
      null, note -> new LinkSourceWithinSameLinkTypeQuizFactory(note)),
  CLOZE_LINK_TARGET(null, note -> new ClozeLinkTargetQuizFactory(note)),
  DESCRIPTION_LINK_TARGET(null, note -> new DescriptionLinkTargetQuizFactory(note)),
  WHICH_SPEC_HAS_INSTANCE(null, note -> new WhichSpecHasInstanceQuizFactory(note)),
  FROM_SAME_PART_AS(null, FromSamePartAsQuizFactory::new),
  FROM_DIFFERENT_PART_AS(null, FromDifferentPartAsQuizFactory::new),
  AI_QUESTION(note -> new AiQuestionFactory(note), null);

  public final Function<Note, QuizQuestionFactory> factory;
  public final Function<LinkingNote, QuizQuestionFactory> factoryForLinkingNote;

  QuestionType(
      Function<Note, QuizQuestionFactory> factory,
      Function<LinkingNote, QuizQuestionFactory> factoryForLinkingNote) {
    this.factory = factory;
    this.factoryForLinkingNote = factoryForLinkingNote;
  }
}
