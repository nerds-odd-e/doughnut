package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionFromSamePartAs;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class FromSamePartAsQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory, SecondaryReviewPointsFactory {

  private final LinkingNote parentGrandLink;
  private Note cachedAnswerLink = null;
  private List<Note> cachedFillingOptions = null;
  private final LinkingNote link;
  private final QuizQuestionServant servant;

  public FromSamePartAsQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    link = note;
    this.servant = servant;
    parentGrandLink = servant.getParentGrandLink(link);
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      List<LinkingNote> remoteCousins =
          servant.getCousinLinksAvoidingSiblings(link, parentGrandLink);
      cachedFillingOptions =
          servant.chooseFillingOptionsRandomly(remoteCousins).stream()
              .map(Note::getParent)
              .toList();
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    if (getAnswerLink() == null) return null;
    return getAnswerLink().getParent();
  }

  @Override
  public LinkingNote getCategoryLink() {
    return parentGrandLink;
  }

  protected Note getAnswerLink() {
    if (cachedAnswerLink == null) {
      List<LinkingNote> backwardPeers =
          servant.getSiblingLinksOfSameLinkTypeHavingReviewPoint(link).toList();
      cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers).orElse(null);
    }
    return cachedAnswerLink;
  }

  @Override
  public QuizQuestionEntity buildQuizQuestion() {
    return new QuizQuestionFromSamePartAs();
  }
}
