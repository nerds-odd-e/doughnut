package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.doughnut.controllers.dto.QuestionContestResult;
import org.junit.jupiter.api.Test;

class QuestionEvaluationTest {
  @Test
  void getQuestionContestResult() {
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.explanation = "what a horrible question!";
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(0);
    assertThat(result.reason, containsString("what a horrible question!"));
    assertThat(result.reason, containsString("Uncleared answer detected."));
  }
}
