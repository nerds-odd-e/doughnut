package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkSourceWithinSameLinkTypeQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final Link link;
  private final QuizQuestionServant servant;
  protected final Note answerNote;
  private List<Link> cachedFillingOptions = null;

  public LinkSourceWithinSameLinkTypeQuizFactory(
      ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.link = reviewPoint.getLink();
    this.servant = servant;
    this.answerNote = link.getSourceNote();
    User user = reviewPoint.getUser();
  }

  @Override
  public List<Link> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      cachedFillingOptions = servant.chooseFromCohortAvoidSiblingsOfSameLinkType(link, answerNote);
    }
    return cachedFillingOptions;
  }

  @Override
  public Link generateAnswer() {
    return link;
  }
}
