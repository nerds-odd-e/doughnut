package com.odde.donut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.Mcq;
import com.odde.donut.testability.MakeMe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionEvaluationTest {
  private QuestionEvaluation questionEvaluation;
  private Mcq mcq;
  private final MakeMe makeMe = MakeMe.makeMeWithoutFactoryService();

  @BeforeEach
  void setup() {
    questionEvaluation = new QuestionEvaluation();
    mcq =
        makeMe
            .anMcq()
            .forNote(null)
            .stem("What is the capital of France?")
            .choices("Paris", "London", "Berlin")
            .correctAnswerIndex(0)
            .inMemoryPlease();
  }

  @Test
  void shouldShowExplanationAndUnclearAnswerWhenNoCorrectChoices() {
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.improvementAdvices = "what a horrible question!";
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcq);
    assertThat(result.advice, containsString("what a horrible question!"));
    assertThat(result.advice, containsString("Unclear answer detected"));
    assertThat(
        result.advice, containsString("original question assume one correct choice of \"Paris\""));
    assertThat(result.advice, containsString("none are correct to the question"));
    assertThat(result.advice, not(containsString("0-based")));
  }

  @Test
  void shouldShowMultipleCorrectChoicesMessage() {
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {1, 2};
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcq);
    assertThat(result.advice, containsString("\"London\", \"Berlin\" are correct to the question"));
  }

  @Test
  void shouldShowLegitimateQuestionMessageWhenAnswerMatches() {
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {0};
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcq);
    assertThat(result.advice, equalTo("This seems to be a legitimate question. Please answer it."));
  }

  @Test
  void shouldHandleOutOfBoundsIndicesInCorrectChoices() {
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {3};
    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcq);
    assertThat(result.advice, containsString("none are correct to the question"));
  }

  @Test
  void shouldQuoteUnknownWhenStoredCorrectIndexIsInvalid() {
    questionEvaluation.feasibleQuestion = true;
    Mcq mcqWithUnknownCorrectChoice =
        makeMe.anMcq().forNote(null).correctAnswerIndex(3).inMemoryPlease();
    QuestionContestResult result =
        questionEvaluation.getQuestionContestResult(mcqWithUnknownCorrectChoice);
    assertThat(
        result.advice,
        containsString("original question assume one correct choice of \"unknown\""));
  }

  @Test
  void shouldHandleNullChoices() {
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.correctChoices = new int[] {1};
    Mcq mcqWithNullChoices =
        makeMe
            .anMcq()
            .forNote(null)
            .stem("What is the capital of France?")
            .choices((List<String>) null)
            .correctAnswerIndex(0)
            .inMemoryPlease();

    QuestionContestResult result = questionEvaluation.getQuestionContestResult(mcqWithNullChoices);
    assertThat(result.advice, equalTo("The question has no choices defined."));
  }
}
