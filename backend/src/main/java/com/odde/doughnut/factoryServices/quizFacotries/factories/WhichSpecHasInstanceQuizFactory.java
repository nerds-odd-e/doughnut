package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionWhichSpecHasInstance;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class WhichSpecHasInstanceQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {
  private LinkingNote cachedInstanceLink = null;
  private List<Note> cachedFillingOptions = null;
  private final LinkingNote link;
  private final QuizQuestionServant servant;

  public WhichSpecHasInstanceQuizFactory(LinkingNote note, QuizQuestionServant servant) {
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

  private LinkingNote getInstanceLink() {
    if (cachedInstanceLink == null) {
      List<LinkingNote> candidates = servant.getLinksFromSameSourceHavingReviewPoint(link).toList();
      cachedInstanceLink = servant.randomizer.chooseOneRandomly(candidates).orElse(null);
    }
    return cachedInstanceLink;
  }

  @Override
  public LinkingNote getCategoryLink() {
    return getInstanceLink();
  }

  @Override
  public QuizQuestionEntity buildQuizQuestion() {
    return new QuizQuestionWhichSpecHasInstance();
  }
}
