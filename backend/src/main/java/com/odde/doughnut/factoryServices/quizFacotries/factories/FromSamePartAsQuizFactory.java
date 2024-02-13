package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionFromSamePartAs;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class FromSamePartAsQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {

  private LinkingNote parentGrandLink;
  private Note cachedAnswerLink = null;
  private List<Note> cachedFillingOptions = null;
  private final LinkingNote link;

  public FromSamePartAsQuizFactory(LinkingNote note) {
    link = note;
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionFromSamePartAs quizQuestionFromSamePartAs = new QuizQuestionFromSamePartAs();
    quizQuestionFromSamePartAs.setNote(link);
    this.parentGrandLink = servant.getParentGrandLink(link);
    quizQuestionFromSamePartAs.setCategoryLink(parentGrandLink);
    return quizQuestionFromSamePartAs;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
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
  public Note generateAnswer(QuizQuestionServant servant) {
    if (getAnswerLink(servant) == null) return null;
    return getAnswerLink(servant).getParent();
  }

  protected Note getAnswerLink(QuizQuestionServant servant) {
    if (cachedAnswerLink == null) {
      List<LinkingNote> backwardPeers =
          servant.getSiblingLinksOfSameLinkTypeHavingReviewPoint(link).toList();
      cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers).orElse(null);
    }
    return cachedAnswerLink;
  }
}
