package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionFromDifferentPartAs;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {

  private LinkingNote parentGrandLink;
  private final LinkingNote link;

  public FromDifferentPartAsQuizFactory(LinkingNote note) {
    link = note;
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    parentGrandLink = servant.getParentGrandLink(link);
    QuizQuestionFromDifferentPartAs quizQuestion = new QuizQuestionFromDifferentPartAs();
    quizQuestion.setCategoryLink(parentGrandLink);
    return quizQuestion;
  }

  @Override
  public int minimumOptionCount() {
    return 3;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    if (parentGrandLink == null) {
      return null;
    }
    List<Note> cousinLinks =
        servant.getSiblingLinksOfSameLinkTypeHavingReviewPoint(link).collect(Collectors.toList());
    return servant.chooseFillingOptionsRandomly(cousinLinks).stream()
        .map(Note::getParent)
        .collect(Collectors.toList());
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return servant
        .randomizer
        .chooseOneRandomly(servant.getCousinLinksAvoidingSiblings(link, parentGrandLink))
        .map(Note::getParent)
        .orElse(null);
  }
}
