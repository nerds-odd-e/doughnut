package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.LinksOfANote;
import com.odde.doughnut.models.NoteViewer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  public LinksOfANote hintLinks() {
    Map<LinkType, LinkViewed> collect =
        new NoteViewer(reviewPoint.getUser(), reviewPoint.getNote())
            .getAllLinks().entrySet().stream()
                .filter(x -> LinkType.openTypes().anyMatch((y) -> x.getKey().equals(y)))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    LinksOfANote links = new LinksOfANote();
    links.setLinks(collect);
    return links;
  }

  @Override
  public List<Note> knownRightAnswers() {
    return List.of(reviewPoint.getNote());
  }
}
