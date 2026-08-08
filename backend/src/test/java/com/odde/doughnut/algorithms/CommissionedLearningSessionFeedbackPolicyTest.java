package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX;
import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommissionedLearningSessionFeedbackPolicyTest {

  @ParameterizedTest
  @CsvSource({
    "5, 112", "4, 110", "3, 108", "2, 100", "1, 100", "0, 100",
  })
  void applyScoreFromInitialLevel(int score, float expected) {
    assertThat(
        CommissionedLearningSessionFeedbackPolicy.applyScore(DEFAULT_FORGETTING_CURVE_INDEX, score),
        is(expected));
  }

  @ParameterizedTest
  @CsvSource({
    "5, 132", "4, 130", "3, 128", "2, 116", "1, 110", "0, 100",
  })
  void applyScoreFromElevatedLevel(int score, float expected) {
    float elevatedIndex =
        DEFAULT_FORGETTING_CURVE_INDEX + DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2f;
    assertThat(
        CommissionedLearningSessionFeedbackPolicy.applyScore(elevatedIndex, score), is(expected));
  }

  @Test
  void applyScoreLeavesIndexUnchangedForInvalidScore() {
    float unchanged = CommissionedLearningSessionFeedbackPolicy.applyScore(120f, 9);
    assertThat(unchanged, is(120f));
  }
}
