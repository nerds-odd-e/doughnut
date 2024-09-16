package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.AssessmentQuestionInstance;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.ReviewQuestionInstance;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.ArrayList;
import java.util.List;

public class AssessmentAttemptBuilder extends EntityBuilder<AssessmentAttempt> {
  List<ReviewQuestionInstanceBuilder> reviewQuestionInstanceBuilders = null;

  public AssessmentAttemptBuilder(MakeMe makeMe, AssessmentAttempt assessmentAttempt) {
    super(makeMe, assessmentAttempt);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (reviewQuestionInstanceBuilders != null) {
      reviewQuestionInstanceBuilders.forEach(
          rqb -> {
            ReviewQuestionInstance rq = rqb.please(needPersist);
            AssessmentQuestionInstance aq = new AssessmentQuestionInstance();
            aq.setReviewQuestionInstance(rq);
            aq.setAssessmentAttempt(entity);
            entity.getAssessmentQuestionInstances().add(aq);
          });
    }

    if (entity.getNotebook() == null) {
      entity.setNotebook(makeMe.aNotebook().please(needPersist));
    }
  }

  public AssessmentAttemptBuilder score(int totalQuestions, int correctAnswers) {
    this.entity.setTotalQuestionCount(totalQuestions);
    this.entity.setAnswersCorrect(correctAnswers);
    return this;
  }

  public AssessmentAttemptBuilder withOneQuestion() {
    return withNQuestions(1);
  }

  public AssessmentAttemptBuilder withNQuestions(int n) {
    this.reviewQuestionInstanceBuilders = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      this.reviewQuestionInstanceBuilders.add(makeMe.aReviewQuestionInstance());
    }
    return this;
  }

  public AssessmentAttemptBuilder notebook(Notebook notebook) {
    this.entity.setNotebook(notebook);
    return this;
  }
}
