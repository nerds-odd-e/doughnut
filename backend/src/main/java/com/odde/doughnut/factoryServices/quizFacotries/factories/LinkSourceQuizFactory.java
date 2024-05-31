package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkSourceQuizFactory extends QuestionOptionsFactory {
  protected final LinkingNote link;
  private List<Note> cachedFillingOptions = null;

  public LinkSourceQuizFactory(LinkingNote link) {
    super(link);
    this.link = link;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    if (cachedFillingOptions == null) {
      cachedFillingOptions = servant.chooseFromCohortAvoidSiblings(link);
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
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
