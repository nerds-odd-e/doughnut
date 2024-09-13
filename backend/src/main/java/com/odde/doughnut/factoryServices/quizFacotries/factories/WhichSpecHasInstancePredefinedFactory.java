package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import java.util.List;

public class WhichSpecHasInstancePredefinedFactory extends QuestionOptionsFactory {
  private Note instanceLink = null;
  private List<Note> cachedFillingOptions = null;
  private final Note link;

  public WhichSpecHasInstancePredefinedFactory(Note note, PredefinedQuestionServant servant) {
    super(note, servant);
    this.link = note;
  }

  @Override
  public void findCategoricalLink() {
    List<Note> candidates = servant.getLinksFromSameSourceHavingReviewPoint(link).toList();
    instanceLink = servant.randomizer.chooseOneRandomly(candidates).orElse(null);
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions != null) {
      return cachedFillingOptions;
    }
    this.cachedFillingOptions = servant.chooseBackwardPeers(instanceLink, link);
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer() {
    if (instanceLink == null) return null;
    return instanceLink.getParent();
  }

  public String getStem() {
    return "<p>Which one is "
        + link.getLinkType().label
        + " <mark>"
        + link.getTargetNote().getTopicConstructor()
        + "</mark> <em>and</em> is "
        + instanceLink.getLinkType().label
        + " <mark>"
        + instanceLink.getTargetNote().getTopicConstructor()
        + "</mark>:";
  }
}
