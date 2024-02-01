package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class WhichSpecHasInstanceQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {
  private Note cachedInstanceLink = null;
  private List<Note> cachedFillingOptions = null;
  private final Note link;
  private final QuizQuestionServant servant;

  public WhichSpecHasInstanceQuizFactory(Note note, QuizQuestionServant servant) {
    this.link = note;
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
    Note instanceLink = getInstanceLink();
    if (instanceLink == null) return null;
    return instanceLink.getParent();
  }

  private Note getInstanceLink() {
    if (cachedInstanceLink == null) {
      List<Note> candidates = servant.getLinksFromSameSourceHavingReviewPoint(link).toList();
      cachedInstanceLink = servant.randomizer.chooseOneRandomly(candidates).orElse(null);
    }
    return cachedInstanceLink;
  }

  @Override
  public Note getCategoryLink() {
    return getInstanceLink();
  }
}
