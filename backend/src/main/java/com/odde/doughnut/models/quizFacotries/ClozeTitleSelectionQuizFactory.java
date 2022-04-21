package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class ClozeTitleSelectionQuizFactory extends ClozeDescriptonQuizFactory
    implements QuestionOptionsFactory {
  public ClozeTitleSelectionQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
  }

  @Override
  public Note generateAnswerNote() {
    return answerNote;
  }

  @Override
  public List<Note> generateFillingOptions() {
    return servant.chooseFromCohort(answerNote, n -> !n.equals(answerNote));
  }

  @Override
  public boolean isValidQuestion() {
    return !Strings.isEmpty(reviewPoint.getNote().getTextContent().getDescription());
  }

  @Override
  public int minimumOptionCount() {
    return 1;
  }
}
