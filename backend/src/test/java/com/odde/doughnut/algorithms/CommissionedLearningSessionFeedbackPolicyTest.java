package com.odde.doughnut.algorithms;

import static com.odde.doughnut.entities.ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.SessionItem;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommissionedLearningSessionFeedbackPolicyTest {

  @Autowired MakeMe makeMe;

  private Timestamp recordedAt;

  @BeforeEach
  void setUp() {
    recordedAt = makeMe.aTimestamp().of(1, 9).please();
  }

  @Test
  void scoreFiveSchedulesLaterThanScoreOneFromSameStartingState() {
    MemoryTracker highScoreTracker = commissionedTrackerAtInitialLevel();
    MemoryTracker lowScoreTracker = commissionedTrackerAtInitialLevel();

    highScoreTracker.recordCommissionedFeedback(recordedAt, 5);
    lowScoreTracker.recordCommissionedFeedback(recordedAt, 1);

    assertThat(highScoreTracker.getNextRecallAt(), greaterThan(lowScoreTracker.getNextRecallAt()));
  }

  @Test
  void scoreZeroSchedulesStrictlyAfterRecordedAt() {
    MemoryTracker tracker = commissionedTrackerAtInitialLevel();

    tracker.recordCommissionedFeedback(recordedAt, 0);

    assertTrue(tracker.getNextRecallAt().after(recordedAt));
  }

  @Test
  void applyScoreNeverDropsBelowInitialLevel() {
    float reduced = CommissionedLearningSessionFeedbackPolicy.applyScore(100f, 1);
    assertThat(reduced, is(DEFAULT_FORGETTING_CURVE_INDEX));
  }

  @Test
  void amendRegradeFromSnapshotMatchesFreshScoreFourNotCompoundOnScoreOne() {
    MemoryTracker amendedTracker = commissionedTrackerAtInitialLevel();
    MemoryTracker freshScoreFourTracker = commissionedTrackerAtInitialLevel();

    float preSessionIndex = amendedTracker.getForgettingCurveIndex();
    int preSessionRecallCount = amendedTracker.getRecallCount();

    amendedTracker.recordCommissionedFeedback(recordedAt, 1);

    SessionItem snapshotFixture = new SessionItem();
    snapshotFixture.setPreSessionForgettingCurveIndex(preSessionIndex);
    snapshotFixture.setPreSessionRecallCount(preSessionRecallCount);

    amendedTracker.restorePreSessionSnapshot(snapshotFixture);
    amendedTracker.recordCommissionedFeedback(recordedAt, 4);

    freshScoreFourTracker.recordCommissionedFeedback(recordedAt, 4);

    assertThat(amendedTracker.getRecallCount(), is(1));
    assertThat(amendedTracker.getNextRecallAt(), is(freshScoreFourTracker.getNextRecallAt()));
  }

  private MemoryTracker commissionedTrackerAtInitialLevel() {
    var user = makeMe.aUser().withSpaceIntervals("1, 2, 4, 8").please();
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    return makeMe
        .aMemoryTrackerFor(note)
        .commissioned()
        .by(user)
        .forgettingCurveAndNextRecallAt(DEFAULT_FORGETTING_CURVE_INDEX)
        .please();
  }
}
