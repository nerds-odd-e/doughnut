package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.Randomizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GeneratedQuestionPostProcessorTest {

  @Test
  void preservesChoiceOrderWhenChoicesMayNotBeShuffled() {
    TestabilitySettings testabilitySettings = mock(TestabilitySettings.class);
    GeneratedQuestionPostProcessor postProcessor =
        new GeneratedQuestionPostProcessor(testabilitySettings);
    MCQWithAnswer originalQuestion =
        new MCQWithAnswer(
            new MultipleChoicesQuestion(
                "Which ordered choice is correct?",
                List.of("first choice", "second choice", "third choice")),
            1,
            false,
            "focus",
            "rationale");

    MCQWithAnswer result = postProcessor.postProcess(originalQuestion);

    assertThat(result, equalTo(originalQuestion));
  }

  @Test
  void preservesCorrectChoiceIndexWhenShuffledChoicesHaveDuplicateText() {
    TestabilitySettings testabilitySettings = mock(TestabilitySettings.class);
    when(testabilitySettings.getRandomizer()).thenReturn(new ReorderingRandomizer(0, 2, 1, 3));
    GeneratedQuestionPostProcessor postProcessor =
        new GeneratedQuestionPostProcessor(testabilitySettings);
    MCQWithAnswer originalQuestion =
        new MCQWithAnswer(
            new MultipleChoicesQuestion(
                "Which duplicate answer is the intended solution?",
                List.of("same answer", "different answer", "same answer", "last answer")),
            2,
            true,
            "focus",
            "rationale");

    MCQWithAnswer result = postProcessor.postProcess(originalQuestion);

    assertThat(
        result.getQuestion().getResponseChoices(),
        equalTo(List.of("same answer", "same answer", "different answer", "last answer")));
    assertThat(result.getSolutionChoiceIndex(), equalTo(1));
  }

  private static class ReorderingRandomizer implements Randomizer {
    private final int[] order;

    private ReorderingRandomizer(int... order) {
      this.order = order;
    }

    @Override
    public <T> List<T> shuffle(List<T> list) {
      List<T> shuffled = new ArrayList<>();
      for (int index : order) {
        shuffled.add(list.get(index));
      }
      return shuffled;
    }

    @Override
    public <T> Optional<T> chooseOneRandomly(List<T> list) {
      return Optional.empty();
    }

    @Override
    public int randomInteger(int min, int max) {
      return min;
    }
  }
}
