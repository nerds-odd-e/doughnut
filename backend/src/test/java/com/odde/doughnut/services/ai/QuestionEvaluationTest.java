package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.doughnut.controllers.dto.ReviewQuestionContestResult;
import org.junit.jupiter.api.Test;

class QuestionEvaluationTest {
  @Test
  void getReviewQuestionContestResult() {
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.comment = "what a horrible question!";
    ReviewQuestionContestResult result = questionEvaluation.getReviewQuestionContestResult(0);
    assertThat(result.reason, containsString("what a horrible question!"));
    assertThat(result.reason, containsString("Uncleared answer detected."));
  }
}
