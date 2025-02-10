package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import com.odde.doughnut.factoryServices.quizFacotries.factories.*;
import java.util.function.BiFunction;

public enum LinkQuestionType {
  LINK_SOURCE(LinkSourcePredefinedFactory::new),
  LINK_SOURCE_WITHIN_SAME_LINK_TYPE(LinkSourceWithinSameLinkTypePredefinedFactory::new),
  CLOZE_LINK_TARGET(ClozeLinkTargetPredefinedFactory::new),
  DESCRIPTION_LINK_TARGET(DescriptionLinkTargetPredefinedFactory::new),
  WHICH_SPEC_HAS_INSTANCE(WhichSpecHasInstancePredefinedFactory::new),
  FROM_DIFFERENT_PART_AS(FromDifferentPartAsPredefinedFactory::new);

  public final BiFunction<Note, PredefinedQuestionServant, PredefinedQuestionFactory>
      factoryForLinkingNote;

  LinkQuestionType(
      BiFunction<Note, PredefinedQuestionServant, PredefinedQuestionFactory>
          factoryForLinkingNote) {
    this.factoryForLinkingNote = factoryForLinkingNote;
  }
}
