package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.NoteViewer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ClozeDescriptonQuizPresenter implements QuizQuestionPresenter {
  private final ReviewPoint reviewPoint;

  public ClozeDescriptonQuizPresenter(QuizQuestion quizQuestion) {
    this.reviewPoint = quizQuestion.getReviewPoint();
  }

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public String instruction() {
    return reviewPoint.getNote().getClozeDescription();
  }

  @Override
  public Map<Link.LinkType, LinkViewed> hintLinks() {
    return new NoteViewer(reviewPoint.getUser(), reviewPoint.getNote())
        .getAllLinks().entrySet().stream()
            .filter(x -> Link.LinkType.openTypes().anyMatch((y) -> x.getKey().equals(y)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public List<Note> knownRightAnswers() {
    return List.of(reviewPoint.getNote());
  }
}
