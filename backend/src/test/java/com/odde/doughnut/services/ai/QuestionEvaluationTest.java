package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.doughnut.controllers.json.QuizQuestionContestResult;
import org.junit.jupiter.api.Test;

class QuestionEvaluationTest {
  @Test
  void getQuizQuestionContestResult() {
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.comment = "what a horrible question!";
    QuizQuestionContestResult result = questionEvaluation.getQuizQuestionContestResult(0);
    assertThat(result.reason, containsString("what a horrible question!"));
    assertThat(result.reason, containsString("Uncleared answer detected."));
  }
}
