package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import java.util.List;

public class LinkSourceQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final Link link;
  protected final Note answerNote;
  private final User user;
  private List<Note> cachedFillingOptions = null;

  public LinkSourceQuizFactory(ReviewPoint reviewPoint) {
    this.link = reviewPoint.getLink();
    this.answerNote = link.getSourceNote();
    this.user = reviewPoint.getUser();
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    if (cachedFillingOptions == null) {
      List<Note> cousinOfSameLinkType = link.getCousinsOfSameLinkType(user);
      cachedFillingOptions =
          servant.chooseFromCohort(
              answerNote,
              n ->
                  !n.equals(answerNote)
                      && !n.equals(link.getTargetNote())
                      && !cousinOfSameLinkType.contains(n));
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswerNote(QuizQuestionServant servant) {
    return answerNote;
  }

  @Override
  public int minimumOptionCount() {
    return 2;
  }

  @Override
  public List<Note> knownRightAnswers() {
    return List.of(answerNote);
  }
}
