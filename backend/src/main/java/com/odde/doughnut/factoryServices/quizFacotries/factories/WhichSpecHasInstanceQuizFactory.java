package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class WhichSpecHasInstanceQuizFactory extends QuestionOptionsFactory {
  private LinkingNote instanceLink = null;
  private List<Note> cachedFillingOptions = null;
  private final LinkingNote link;

  public WhichSpecHasInstanceQuizFactory(LinkingNote note) {
    this.link = note;
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    List<LinkingNote> candidates = servant.getLinksFromSameSourceHavingReviewPoint(link).toList();
    instanceLink = servant.randomizer.chooseOneRandomly(candidates).orElse(null);
    QuizQuestionEntity quizQuestionWhichSpecHasInstance = new QuizQuestionEntity();
    quizQuestionWhichSpecHasInstance.setNote(link);
    return quizQuestionWhichSpecHasInstance;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    if (cachedFillingOptions != null) {
      return cachedFillingOptions;
    }
    this.cachedFillingOptions = servant.chooseBackwardPeers(instanceLink, link);
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
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
