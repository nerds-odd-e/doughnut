package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizPresenter implements QuizQuestionPresenter {
  protected final Link link;
  private final User user;
  private Link categoryLink;

  public FromDifferentPartAsQuizPresenter(QuizQuestionEntity quizQuestion) {
    this.user = quizQuestion.getReviewPoint().getUser();
    this.link = quizQuestion.getReviewPoint().getLink();
    this.categoryLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String instruction() {
    return "<p>Which one <mark>is "
        + link.getLinkTypeLabel()
        + "</mark> a <em>DIFFERENT</em> "
        + categoryLink.getLinkType().nameOfSource
        + " of <mark>"
        + categoryLink.getTargetNote().getTitle()
        + "</mark> than:";
  }

  @Override
  public String mainTopic() {
    return link.getSourceNote().getTitle();
  }

  public List<Note> knownRightAnswers() {
    ParentGrandLinkHelperImpl parentGrandLinkHelper =
        new ParentGrandLinkHelperImpl(user, link, categoryLink);
    return parentGrandLinkHelper.getCousinLinksAvoidingSiblings().stream()
        .map(Link::getSourceNote)
        .collect(Collectors.toList());
  }

  @Override
  public boolean isAnswerCorrect(String spellingAnswer) {
    return knownRightAnswers().stream()
        .anyMatch(correctAnswerNote -> correctAnswerNote.matchAnswer(spellingAnswer));
  }
}
