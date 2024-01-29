package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WhichSpecHasInstanceQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {
  private Thing cachedInstanceLink = null;
  private List<Note> cachedFillingOptions = null;
  private final Thing link;
  private final QuizQuestionServant servant;

  public WhichSpecHasInstanceQuizFactory(Thing thing, QuizQuestionServant servant) {
    this.link = thing;
    this.servant = servant;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions != null) {
      return cachedFillingOptions;
    }
    this.cachedFillingOptions = servant.chooseBackwardPeers(cachedInstanceLink, link);
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    Thing instanceLink = getInstanceLink();
    if (instanceLink == null) return null;
    return instanceLink.getParentNote();
  }

  private Thing getInstanceLink() {
    if (cachedInstanceLink == null) {
      Stream<Thing> candidates = servant.getLinksFromSameSourceHavingReviewPoint(link);
      cachedInstanceLink =
          servant
              .randomizer
              .chooseOneRandomly(candidates.collect(Collectors.toList()))
              .orElse(null);
    }
    return cachedInstanceLink;
  }

  @Override
  public Thing getCategoryLink() {
    return getInstanceLink();
  }
}
