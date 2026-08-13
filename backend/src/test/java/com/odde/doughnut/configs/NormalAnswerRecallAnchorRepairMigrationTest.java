package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.sql.Timestamp;
import org.junit.jupiter.api.Test;

class NormalAnswerRecallAnchorRepairMigrationTest extends RecallAnchorRepairMigrationTestSupport {

  private static final String MIGRATION =
      "db/migration/V300000248__repair_memory_tracker_recall_anchor_from_answers.sql";

  @Test
  void doesNotRepairAnchorsWhenPlaceholderDefaultsToNoOp() throws Exception {
    Timestamp originalAnchor = timestamp("2026-01-01 08:00:00");
    long trackerId = insertMemoryTracker(originalAnchor, timestamp("2026-02-01 08:00:00"));
    insertAnswer(trackerId, timestamp("2026-01-03 08:00:00"), true, null);

    assertThat(runRepair(MIGRATION, "1=0"), is(0));

    assertThat(readTrackerState(trackerId).lastRecalledAt(), is(originalAnchor));
  }

  @Test
  void repairsToLatestNormalAnswerWithoutChangingDueAndIsIdempotent() throws Exception {
    Timestamp originalAnchor = timestamp("2026-01-01 08:00:00");
    Timestamp due = timestamp("2026-02-01 08:00:00");
    Timestamp latestNormalAnswer = timestamp("2026-01-03 08:00:00");
    long trackerId = insertMemoryTracker(originalAnchor, due);
    insertAnswer(trackerId, timestamp("2026-01-02 08:00:00"), true, null);
    insertAnswer(trackerId, latestNormalAnswer, false, null);
    insertAnswer(trackerId, timestamp("2026-01-04 08:00:00"), true, "OVERLAP");

    assertThat(runRepair(MIGRATION, "1=1"), is(1));
    TrackerState repaired = readTrackerState(trackerId);
    assertThat(repaired.lastRecalledAt(), is(latestNormalAnswer));
    assertThat(repaired.nextRecallAt(), is(due));

    assertThat(runRepair(MIGRATION, "1=1"), is(0));
    assertThat(readTrackerState(trackerId), is(repaired));
  }

  @Test
  void preservesAnAnchorLaterThanTheLatestNormalAnswer() throws Exception {
    Timestamp currentAnchor = timestamp("2026-01-05 08:00:00");
    long trackerId = insertMemoryTracker(currentAnchor, timestamp("2026-02-01 08:00:00"));
    insertAnswer(trackerId, timestamp("2026-01-03 08:00:00"), true, null);

    assertThat(runRepair(MIGRATION, "1=1"), is(0));

    assertThat(readTrackerState(trackerId).lastRecalledAt(), is(currentAnchor));
  }
}
