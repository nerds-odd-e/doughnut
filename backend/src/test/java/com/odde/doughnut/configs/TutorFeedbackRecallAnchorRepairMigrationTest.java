package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class TutorFeedbackRecallAnchorRepairMigrationTest extends RecallAnchorRepairMigrationTestSupport {

  private static final String MIGRATION =
      "db/migration/V300000251__repair_memory_tracker_recall_anchor_from_tutor_feedback.sql";

  @Test
  void doesNotRepairAnchorsWhenPlaceholderDefaultsToNoOp() throws Exception {
    Timestamp originalAnchor = timestamp("2026-01-01 08:00:00");
    long trackerId = insertMemoryTracker(originalAnchor, timestamp("2026-02-01 08:00:00"));
    insertTutorFeedback(trackerId, timestamp("2026-01-03 08:00:00"));

    assertThat(runRepair(MIGRATION, "1=0"), is(0));

    assertThat(readTrackerState(trackerId).lastRecalledAt(), is(originalAnchor));
  }

  @Test
  void repairsToLatestTutorFeedbackWithoutChangingDueAndIsIdempotent() throws Exception {
    Timestamp originalAnchor = timestamp("2026-01-01 08:00:00");
    Timestamp due = timestamp("2026-02-01 08:00:00");
    Timestamp latestFeedback = timestamp("2026-01-03 08:00:00");
    long trackerId = insertMemoryTracker(originalAnchor, due);
    insertTutorFeedback(trackerId, timestamp("2026-01-02 08:00:00"));
    insertTutorFeedback(trackerId, latestFeedback);

    assertThat(runRepair(MIGRATION, "1=1"), is(1));
    TrackerState repaired = readTrackerState(trackerId);
    assertThat(repaired.lastRecalledAt(), is(latestFeedback));
    assertThat(repaired.nextRecallAt(), is(due));

    assertThat(runRepair(MIGRATION, "1=1"), is(0));
    assertThat(readTrackerState(trackerId), is(repaired));
  }

  @Test
  void preservesAnAnchorLaterThanTheLatestTutorFeedback() throws Exception {
    Timestamp currentAnchor = timestamp("2026-01-05 08:00:00");
    long trackerId = insertMemoryTracker(currentAnchor, timestamp("2026-02-01 08:00:00"));
    insertTutorFeedback(trackerId, timestamp("2026-01-03 08:00:00"));

    assertThat(runRepair(MIGRATION, "1=1"), is(0));

    assertThat(readTrackerState(trackerId).lastRecalledAt(), is(currentAnchor));
  }
}
