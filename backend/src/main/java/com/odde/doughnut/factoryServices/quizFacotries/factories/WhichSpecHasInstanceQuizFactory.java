package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionWhichSpecHasInstance;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class WhichSpecHasInstanceQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory {
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
    QuizQuestionWhichSpecHasInstance quizQuestionWhichSpecHasInstance =
        new QuizQuestionWhichSpecHasInstance();
    quizQuestionWhichSpecHasInstance.setNote(link);
    quizQuestionWhichSpecHasInstance.setCategoryLink(instanceLink);
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
}
