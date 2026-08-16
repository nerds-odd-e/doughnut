package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.ASSIMILATE_STABILITY_HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommissionedLearningSessionFeedbackPolicyTest {

  @ParameterizedTest
  @CsvSource({
    "5, 29", "4, 24", "3, 19", "2, 0", "1, 0", "0, 0",
  })
  void applyScoreFromInitialLevel(int score, float expected) {
    assertThat(
        CommissionedLearningSessionFeedbackPolicy.applyScore(ASSIMILATE_STABILITY_HOURS, score),
        is(expected));
  }

  @ParameterizedTest
  @CsvSource({
    "5, 77", "3, 67", "2, 38", "1, 24", "0, 0",
  })
  void applyScoreFromElevatedLevel(int score, float expected) {
    float elevatedHours = 48f;
    assertThat(
        CommissionedLearningSessionFeedbackPolicy.applyScore(elevatedHours, score), is(expected));
  }

  @Test
  void applyScoreLeavesHoursUnchangedForInvalidScore() {
    float unchanged = CommissionedLearningSessionFeedbackPolicy.applyScore(48f, 9);
    assertThat(unchanged, is(48f));
  }
}
