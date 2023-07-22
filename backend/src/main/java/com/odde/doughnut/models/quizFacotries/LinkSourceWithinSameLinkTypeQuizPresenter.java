package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.json.QuizQuestion;
import java.util.List;
import java.util.stream.Stream;

public class LinkSourceWithinSameLinkTypeQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Link link;

  public LinkSourceWithinSameLinkTypeQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getReviewPoint().getLink();
  }

  @Override
  public String mainTopic() {
    return link.getTargetNote().getTitle();
  }

  @Override
  public String instruction() {
    return "Which one <em>is immediately " + link.getLinkTypeLabel() + "</em>:";
  }

  @Override
  protected List<QuizQuestion.Option> getOptionsFromThings(Stream<Thing> noteStream) {
    return noteStream
        .map(
            thing -> {
              QuizQuestion.Option option = new QuizQuestion.Option();
              option.setNoteId(thing.getLink().getSourceNote().getId());
              option.setDisplay(thing.getLink().getClozeSource().cloze());
              return option;
            })
        .toList();
  }
}
