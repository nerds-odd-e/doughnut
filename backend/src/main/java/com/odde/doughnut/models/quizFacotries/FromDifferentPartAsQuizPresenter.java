package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizPresenter extends QuizQuestionWithOptionsPresenter {
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
  public boolean isAnswerCorrect(Answer answer) {
    return knownRightAnswers().stream()
        .anyMatch(correctAnswerNote -> correctAnswerNote.getId().equals(answer.getAnswerNoteId()));
  }
}
