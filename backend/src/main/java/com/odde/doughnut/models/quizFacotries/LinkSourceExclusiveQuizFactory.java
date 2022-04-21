package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import java.util.ArrayList;
import java.util.List;

public class LinkSourceExclusiveQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  private final Link link;
  private QuizQuestionServant servant;
  private final ReviewPoint reviewPoint;
  private List<Note> cachedFillingOptions = null;
  private Note answerNote = null;

  public LinkSourceExclusiveQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.link = reviewPoint.getLink();
    this.servant = servant;
  }

  @Override
  public List<Note> generateFillingOptions() {
    if (cachedFillingOptions == null) {
      Note sourceNote = link.getSourceNote();
      List<Note> backwardPeers = link.getCousinsOfSameLinkType(reviewPoint.getUser());
      cachedFillingOptions = servant.randomlyChooseAndEnsure(backwardPeers, sourceNote);
    }
    return cachedFillingOptions;
  }

  @Override
  public Note generateAnswerNote() {
    if (answerNote == null) {
      Note note = link.getSourceNote();
      List<Note> siblings = new ArrayList<>(note.getSiblings());
      siblings.removeAll(link.getCousinsOfSameLinkType(reviewPoint.getUser()));
      siblings.remove(link.getTargetNote());
      siblings.remove(link.getSourceNote());
      answerNote = servant.randomizer.chooseOneRandomly(siblings).orElse(null);
    }
    return answerNote;
  }

  @Override
  public int minimumOptionCount() {
    return 2;
  }

  @Override
  public List<Note> allWrongAnswers() {
    return List.of(link.getSourceNote());
  }
}
