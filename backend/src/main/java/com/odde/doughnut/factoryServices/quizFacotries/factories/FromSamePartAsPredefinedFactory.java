package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import java.util.List;

public class FromSamePartAsPredefinedFactory extends QuestionOptionsFactory {

  private Note parentGrandLink;
  private Note cachedAnswerLink = null;
  private List<Note> cachedFillingOptions = null;
  private final Note link;

  public FromSamePartAsPredefinedFactory(Note note, PredefinedQuestionServant servant) {
    super(note, servant);
    link = note;
  }

  @Override
  public void findCategoricalLink() {
    this.parentGrandLink = servant.getParentGrandLink(link);
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      List<Note> remoteCousins = servant.getCousinLinksAvoidingSiblings(link, parentGrandLink);
      cachedFillingOptions =
          servant.chooseFillingOptionsRandomly(remoteCousins).stream()
              .map(Note::getParent)
              .toList();
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    if (getAnswerLink(servant) == null) return null;
    return getAnswerLink(servant).getParent();
  }

  protected Note getAnswerLink(PredefinedQuestionServant servant) {
    if (cachedAnswerLink == null) {
      List<Note> backwardPeers =
          servant.getSiblingLinksOfSameLinkTypeHavingReviewPoint(link).toList();
      cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers).orElse(null);
    }
    return cachedAnswerLink;
  }

  @Override
  public String getStem() {
    return "<p>Which one <mark>is "
        + link.getLinkType().label
        + "</mark> the same "
        + parentGrandLink.getLinkType().nameOfSource
        + " of <mark>"
        + parentGrandLink.getTargetNote().getTopicConstructor()
        + "</mark> as:"
        + "<strong>"
        + link.getParent().getTopicConstructor()
        + "</strong>";
  }
}
