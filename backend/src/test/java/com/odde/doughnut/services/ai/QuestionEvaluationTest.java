package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.dto.QuestionContestResult;
import org.junit.jupiter.api.Test;

class QuestionEvaluationTest {
  @Test
  void shouldShowExplanationAndUnclearAnswerWhenNoCorrectChoices() {
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.explanation = "what a horrible question!";
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(0);
    assertThat(result.reason, containsString("what a horrible question!"));
    assertThat(result.reason, containsString("Unclear answer detected"));
    assertThat(
        result.reason, containsString("original question assume one correct choice index 0"));
    assertThat(result.reason, containsString("none are correct to the question"));
  }

  @Test
  void shouldShowMultipleCorrectChoicesMessage() {
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {1, 2};
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(0);
    assertThat(
        result.reason, containsString("original question assume one correct choice index 0"));
    assertThat(result.reason, containsString("1, 2 are correct to the question"));
  }

  @Test
  void shouldShowLegitimateQuestionMessageWhenAnswerMatches() {
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {0};
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(0);
    assertEquals("This seems to be a legitimate question. Please answer it.", result.reason);
  }
}
