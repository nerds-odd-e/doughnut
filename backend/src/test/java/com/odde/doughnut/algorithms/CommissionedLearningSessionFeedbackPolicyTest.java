package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.ASSIMILATE_STABILITY_HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class CommissionedLearningSessionFeedbackPolicyTest {

  @Test
  void applyScoreFromInitialLevel() {
    assertThat(
        CommissionedLearningSessionFeedbackPolicy.applyScore(ASSIMILATE_STABILITY_HOURS, 2),
        is(ASSIMILATE_STABILITY_HOURS));
  }

  @Test
  void applyScoreFromElevatedLevel() {
    float elevatedHours = 48f;
    assertThat(CommissionedLearningSessionFeedbackPolicy.applyScore(elevatedHours, 2), is(38f));
  }

  @Test
  void applyScoreLeavesHoursUnchangedForInvalidScore() {
    float unchanged = CommissionedLearningSessionFeedbackPolicy.applyScore(48f, 9);
    assertThat(unchanged, is(48f));
  }
}
