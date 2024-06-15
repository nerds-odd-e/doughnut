package com.odde.doughnut.services;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import java.util.function.BiFunction;

public enum LinkQuestionType {
  LINK_TARGET(LinkTargetQuizFactory::new),
  LINK_SOURCE(LinkSourceQuizFactory::new),
  LINK_SOURCE_WITHIN_SAME_LINK_TYPE(LinkSourceWithinSameLinkTypeQuizFactory::new),
  CLOZE_LINK_TARGET(ClozeLinkTargetQuizFactory::new),
  DESCRIPTION_LINK_TARGET(DescriptionLinkTargetQuizFactory::new),
  WHICH_SPEC_HAS_INSTANCE(WhichSpecHasInstanceQuizFactory::new),
  FROM_SAME_PART_AS(FromSamePartAsQuizFactory::new),
  FROM_DIFFERENT_PART_AS(FromDifferentPartAsQuizFactory::new);

  public final BiFunction<LinkingNote, QuizQuestionServant, QuizQuestionFactory>
      factoryForLinkingNote;

  LinkQuestionType(
      BiFunction<LinkingNote, QuizQuestionServant, QuizQuestionFactory> factoryForLinkingNote) {
    this.factoryForLinkingNote = factoryForLinkingNote;
  }
}
