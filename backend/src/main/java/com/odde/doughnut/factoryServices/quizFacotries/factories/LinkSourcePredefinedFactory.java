package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import java.util.List;

public class LinkSourcePredefinedFactory extends QuestionOptionsFactory {
  protected final Note link;
  private List<Note> cachedFillingOptions = null;

  public LinkSourcePredefinedFactory(Note link, PredefinedQuestionServant servant) {
    super(link, servant);
    this.link = link;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      cachedFillingOptions = servant.chooseFromCohortAvoidSiblings(link);
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    return link.getParent();
  }

  @Override
  public String getStem() {
    return "Which one <em>is immediately "
        + link.getLinkType().label
        + "</em>:"
        + "<br><br>"
        + "<strong>"
        + link.getTargetNote().getTopicConstructor()
        + "</strong>";
  }
}
