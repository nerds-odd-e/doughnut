package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkTargetQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final Link link;
  private QuizQuestionServant servant;
  protected final Note answerNote;
  private final User user;
  private List<Note> cachedFillingOptions = null;

  public LinkTargetQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.link = reviewPoint.getLink();
    this.servant = servant;
    this.answerNote = link.getTargetNote();
    this.user = reviewPoint.getUser();
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      List<Note> uncles = link.getPiblingOfTheSameLinkType(user);
      cachedFillingOptions =
          servant.chooseFromCohort(
              answerNote,
              n -> !n.equals(answerNote) && !n.equals(link.getSourceNote()) && !uncles.contains(n));
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    return answerNote;
  }
}
