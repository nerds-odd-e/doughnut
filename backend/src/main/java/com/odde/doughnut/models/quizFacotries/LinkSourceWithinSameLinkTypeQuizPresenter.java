package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.json.QuizQuestion;

public class LinkSourceWithinSameLinkTypeQuizPresenter implements QuizQuestionPresenter {
  protected final Link link;

  public LinkSourceWithinSameLinkTypeQuizPresenter(QuizQuestionEntity quizQuestion) {
    this.link = quizQuestion.getReviewPoint().getLink();
  }

  @Override
  public String mainTopic() {
    return link.getTargetNote().getTitle();
  }

  @Override
  public String instruction() {
    return "Which one <em>is immediately " + link.getLinkTypeLabel() + "</em>:";
  }

  @Override
  public QuizQuestion.OptionCreator optionCreator() {
    return new QuizQuestion.ClozeLinkOptionCreator();
  }

  @Override
  public boolean isAnswerCorrect(String spellingAnswer) {
    return link.getSourceNote().matchAnswer(spellingAnswer);
  }
}
