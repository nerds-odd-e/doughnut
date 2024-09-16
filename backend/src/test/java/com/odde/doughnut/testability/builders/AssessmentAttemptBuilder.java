package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.ArrayList;
import java.util.List;

public class AssessmentAttemptBuilder extends EntityBuilder<AssessmentAttempt> {
  List<PredefinedQuestionBuilder> predefinedQuestionBuilders = null;

  public AssessmentAttemptBuilder(MakeMe makeMe, AssessmentAttempt assessmentAttempt) {
    super(makeMe, assessmentAttempt);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (predefinedQuestionBuilders != null) {
      entity.buildQuestions(
          predefinedQuestionBuilders.stream()
              .map(predefinedQuestionBuilder -> predefinedQuestionBuilder.please(needPersist))
              .toList());
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
    this.predefinedQuestionBuilders = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      this.predefinedQuestionBuilders.add(makeMe.aPredefinedQuestion());
    }
    return this;
  }

  public AssessmentAttemptBuilder notebook(Notebook notebook) {
    this.entity.setNotebook(notebook);
    return this;
  }
}
