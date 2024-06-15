package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkTargetQuizFactory extends QuestionOptionsFactory {
  protected final LinkingNote link;
  protected final Note answerNote;
  private List<Note> cachedFillingOptions = null;

  public LinkTargetQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    super(note, servant);
    this.link = note;
    this.answerNote = link.getTargetNote();
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    if (cachedFillingOptions == null) {
      cachedFillingOptions = servant.chooseFromCohortAvoidUncles(link, answerNote);
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return answerNote;
  }

  @Override
  public String getStem() {
    return "<mark>"
        + link.getParent().getTopicConstructor()
        + "</mark> is "
        + link.getLinkType().label
        + ":";
  }
}
