package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory extends QuestionOptionsFactory {

  private LinkingNote parentGrandLink;
  private final LinkingNote link;

  public FromDifferentPartAsQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    super(note, servant);
    link = note;
  }

  @Override
  public void findCategoricalLink() {
    parentGrandLink = servant.getParentGrandLink(link);
  }

  @Override
  public int minimumOptionCount() {
    return 3;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (parentGrandLink == null) {
      return null;
    }
    List<Note> cousinLinks =
        servant.getSiblingLinksOfSameLinkTypeHavingReviewPoint(link).collect(Collectors.toList());
    return servant.chooseFillingOptionsRandomly(cousinLinks).stream()
        .map(Note::getParent)
        .collect(Collectors.toList());
  }

  @Override
  public Note generateAnswer() {
    return servant
        .randomizer
        .chooseOneRandomly(servant.getCousinLinksAvoidingSiblings(link, parentGrandLink))
        .map(Note::getParent)
        .orElse(null);
  }

  @Override
  public String getStem() {
    return "<p>Which one <mark>is "
        + link.getLinkType().label
        + "</mark> a <em>DIFFERENT</em> "
        + parentGrandLink.getLinkType().nameOfSource
        + " of <mark>"
        + parentGrandLink.getTargetNote().getTopicConstructor()
        + "</mark> than:"
        + "<strong>"
        + link.getParent().getTopicConstructor()
        + "</strong>";
  }
}
