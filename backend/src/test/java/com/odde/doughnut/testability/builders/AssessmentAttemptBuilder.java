package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class AssessmentAttemptBuilder extends EntityBuilder<AssessmentAttempt> {
  ReviewQuestionInstanceBuilder reviewQuestionInstanceBuilder = null;

  public AssessmentAttemptBuilder(MakeMe makeMe, AssessmentAttempt assessmentAttempt) {
    super(makeMe, assessmentAttempt);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (reviewQuestionInstanceBuilder != null) {
      ReviewQuestionInstance rq = reviewQuestionInstanceBuilder.please(needPersist);
      AssessmentQuestionInstance aq = new AssessmentQuestionInstance();
      aq.setReviewQuestionInstance(rq);
      aq.setAssessmentAttempt(entity);
      entity.getAssessmentQuestionInstances().add(aq);
    }

    if (entity.getNotebook() == null) {
      entity.setNotebook(makeMe.aNotebook().please(needPersist));
    }
  }

  public AssessmentAttemptBuilder score(int totalQuestions, int correctAnswers) {
    this.entity.setAnswersTotal(totalQuestions);
    this.entity.setAnswersCorrect(correctAnswers);
    return this;
  }

  public AssessmentAttemptBuilder withOneQuestion() {
    this.reviewQuestionInstanceBuilder = makeMe.aReviewQuestionInstance();

    return this;
  }

  public AssessmentAttemptBuilder notebook(Notebook notebook) {
    this.entity.setNotebook(notebook);
    return this;
  }
}
