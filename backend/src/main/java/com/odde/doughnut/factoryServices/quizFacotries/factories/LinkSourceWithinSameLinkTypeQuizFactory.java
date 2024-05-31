package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class LinkSourceWithinSameLinkTypeQuizFactory extends QuestionOptionsFactory {
  protected final LinkingNote link;
  private List<LinkingNote> cachedFillingOptions = null;

  public LinkSourceWithinSameLinkTypeQuizFactory(LinkingNote note) {
    super(note);
    this.link = note;
  }

  @Override
  public List<LinkingNote> generateFillingOptions(QuizQuestionServant servant) {
    if (cachedFillingOptions == null) {
      cachedFillingOptions =
          servant.chooseFromCohortAvoidSiblings(link).stream()
              .flatMap(n -> servant.randomizer.chooseOneRandomly(n.getLinks()).stream())
              .toList();
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return link;
  }

  @Override
  public QuizQuestion.Choice noteToChoice(Note note) {
    QuizQuestion.Choice choice = new QuizQuestion.Choice();
    Note source = note.getParent();
    Note target = note.getTargetNote();
    choice.setDisplay(
        ClozedString.htmlClozedString(source.getTopicConstructor())
            .hide(target.getNoteTitle())
            .clozeTitle());
    return choice;
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
