package com.odde.donut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

class FsrsRetrievabilityTest {
  static final float STABILITY_HOURS = 72f;

  @Test
  void retrievabilityAtElapsedZeroIsOne() {
    assertThat(Fsrs.retrievabilityFromHours(STABILITY_HOURS, 0), closeTo(1.0, 1e-12));
  }

  @Test
  void retrievabilityAtElapsedEqualToStabilityIsRequestedRetention() {
    assertThat(
        Fsrs.retrievabilityFromHours(STABILITY_HOURS, Math.round(STABILITY_HOURS)),
        closeTo(Fsrs.REQUESTED_RETENTION, 1e-12));
  }

  @Test
  void intervalHoursEqualsStabilityAtRequestedRetention() {
    assertThat(Fsrs.intervalHours(STABILITY_HOURS), equalTo(Math.round(STABILITY_HOURS)));
  }
}
