package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionEvaluationTest {
  private QuestionEvaluation questionEvaluation;
  private MCQWithAnswer mcqWithAnswer;
  private MakeMeWithoutDB makeMe;

  @BeforeEach
  void setup() {
    makeMe = new MakeMeWithoutDB();
    questionEvaluation = new QuestionEvaluation();
    mcqWithAnswer =
        makeMe
            .aMCQWithAnswer()
            .stem("What is the capital of France?")
            .choices("Paris", "London", "Berlin")
            .correctChoiceIndex(0)
            .please();
  }

  @Test
  void shouldShowExplanationAndUnclearAnswerWhenNoCorrectChoices() {
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.improvementAdvices = "what a horrible question!";
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcqWithAnswer);
    assertThat(result.advice, containsString("what a horrible question!"));
    assertThat(result.advice, containsString("Unclear answer detected"));
    assertThat(
        result.advice,
        containsString(
            "original question assume one correct choice index (0-based) of 0 (\"Paris\")"));
    assertThat(result.advice, containsString("none are correct to the question"));
  }

  @Test
  void shouldShowMultipleCorrectChoicesMessage() {
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {1, 2};
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcqWithAnswer);
    assertThat(
        result.advice,
        containsString(
            "original question assume one correct choice index (0-based) of 0 (\"Paris\")"));
    assertThat(
        result.advice,
        containsString("1 (\"London\"), 2 (\"Berlin\") are correct to the question"));
  }

  @Test
  void shouldShowLegitimateQuestionMessageWhenAnswerMatches() {
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {0};
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcqWithAnswer);
    assertEquals("This seems to be a legitimate question. Please answer it.", result.advice);
  }

  @Test
  void shouldHandleNullChoicesGracefully() {
    // Reproduce the NullPointerException scenario
    MCQWithAnswer mcqWithNullChoices = new MCQWithAnswer();
    mcqWithNullChoices.setMultipleChoicesQuestion(new MultipleChoicesQuestion("What is the capital?", null));
    mcqWithNullChoices.setCorrectChoiceIndex(0);

    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {1};
    questionEvaluation.improvementAdvices = "Please fix the choices";

    // This should not throw NullPointerException
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcqWithNullChoices);
    assertThat(result.advice, containsString("Please fix the choices"));
  }
}
