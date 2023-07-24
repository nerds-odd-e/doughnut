package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.models.NoteViewer;
import java.util.List;
import java.util.stream.Collectors;

public class LinkSourceWithinSameLinkTypeQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final Link link;
  private final QuizQuestionServant servant;
  protected final Note answerNote;
  private final User user;
  private List<Link> cachedFillingOptions = null;

  public LinkSourceWithinSameLinkTypeQuizFactory(
      ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.link = reviewPoint.getLink();
    this.servant = servant;
    this.answerNote = link.getSourceNote();
    this.user = reviewPoint.getUser();
  }

  @Override
  public List<Link> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      List<Note> linkedSiblingsOfSameLinkType = link.getLinkedSiblingsOfSameLinkType(user);
      cachedFillingOptions =
          servant
              .chooseFromCohort(
                  answerNote,
                  n ->
                      !n.equals(answerNote)
                          && !n.equals(link.getTargetNote())
                          && !linkedSiblingsOfSameLinkType.contains(n)
                          && !new NoteViewer(user, n)
                              .linksOfTypeThroughDirect(List.of(link.getLinkType()))
                              .isEmpty())
              .stream()
              .map(n -> n.getLinks().get(0))
              .collect(Collectors.toList());
    }
    return cachedFillingOptions;
  }

  @Override
  public Link generateAnswer() {
    return link;
  }
}
