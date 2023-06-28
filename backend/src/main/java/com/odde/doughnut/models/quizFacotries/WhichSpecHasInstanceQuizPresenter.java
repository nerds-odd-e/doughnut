package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import java.util.List;

public class WhichSpecHasInstanceQuizPresenter implements QuizQuestionPresenter {
  private Link instanceLink;
  private final Link link;

  public WhichSpecHasInstanceQuizPresenter(QuizQuestionEntity quizQuestion) {
    this.link = quizQuestion.getReviewPoint().getLink();
    this.instanceLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String mainTopic() {
    return null;
  }

  @Override
  public String instruction() {
    return "<p>Which one is "
        + link.getLinkTypeLabel()
        + " <mark>"
        + link.getTargetNote().getTitle()
        + "</mark> <em>and</em> is "
        + instanceLink.getLinkTypeLabel()
        + " <mark>"
        + instanceLink.getTargetNote().getTitle()
        + "</mark>:";
  }

  @Override
  public List<Note> knownRightAnswers() {
    return List.of(link.getSourceNote());
  }
}
