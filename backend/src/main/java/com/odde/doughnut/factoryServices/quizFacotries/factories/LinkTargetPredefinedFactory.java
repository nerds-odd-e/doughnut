package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import java.util.List;

public class LinkTargetPredefinedFactory extends QuestionOptionsFactory {
  protected final LinkingNote link;
  protected final Note answerNote;
  private List<Note> cachedFillingOptions = null;

  public LinkTargetPredefinedFactory(LinkingNote note, PredefinedQuestionServant servant) {
    super(note, servant);
    this.link = note;
    this.answerNote = link.getTargetNote();
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      cachedFillingOptions = servant.chooseFromCohortAvoidUncles(link, answerNote);
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
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
