package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.Randomizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GeneratedQuestionPostProcessorTest {

  @Test
  void preservesChoiceOrderWhenChoicesMayNotBeShuffled() {
    GeneratedQuestionPostProcessor postProcessor =
        new GeneratedQuestionPostProcessor(new TestabilitySettings());
    GeneratedMcq originalQuestion =
        new GeneratedMcq(
            "Which ordered choice is correct?",
            List.of("first choice", "second choice", "third choice"),
            1,
            false,
            "focus",
            "rationale");

    GeneratedMcq result = postProcessor.postProcess(originalQuestion);

    assertThat(result, equalTo(originalQuestion));
  }

  @Test
  void preservesCorrectChoiceIndexWhenShuffledChoicesHaveDuplicateText() {
    GeneratedQuestionPostProcessor postProcessor =
        new GeneratedQuestionPostProcessor(
            new TestabilitySettings() {
              @Override
              public Randomizer getRandomizer() {
                return new ReorderingRandomizer(0, 2, 1, 3);
              }
            });
    GeneratedMcq originalQuestion =
        new GeneratedMcq(
            "Which duplicate answer is the intended solution?",
            List.of("same answer", "different answer", "same answer", "last answer"),
            2,
            true,
            "focus",
            "rationale");

    GeneratedMcq result = postProcessor.postProcess(originalQuestion);

    assertThat(
        result.getResponseChoices(),
        equalTo(List.of("same answer", "same answer", "different answer", "last answer")));
    assertThat(result.getCorrectAnswerIndex(), equalTo(1));
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
