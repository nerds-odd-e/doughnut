package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.User;
import java.util.List;

public class FromSamePartAsQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Link link;
  private final Link categoryLink;
  private final User user;

  public FromSamePartAsQuizPresenter(QuizQuestionEntity quizQuestion) {
    this.user = quizQuestion.getReviewPoint().getUser();
    this.link = quizQuestion.getReviewPoint().getLink();
    this.categoryLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String mainTopic() {
    return link.getSourceNote().getTitle();
  }

  @Override
  public String instruction() {
    return "<p>Which one <mark>is "
        + link.getLinkTypeLabel()
        + "</mark> the same "
        + categoryLink.getLinkType().nameOfSource
        + " of <mark>"
        + categoryLink.getTargetNote().getTitle()
        + "</mark> as:";
  }

  public List<Note> knownRightAnswers() {
    return link.getLinkedSiblingsOfSameLinkType(user);
  }

  @Override
  public boolean isAnswerCorrect(String spellingAnswer) {
    return knownRightAnswers().stream()
        .anyMatch(correctAnswerNote -> correctAnswerNote.matchAnswer(spellingAnswer));
  }
}
