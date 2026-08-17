package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.algorithms.CommissionedLearningSessionFeedbackScheduling;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MemoryTrackerTutorRecallLogBackfillControllerTest
    extends RecallLogBackfillControllerTestBase {
  @Override
  protected String migrationSql() {
    return "db/migration/V300000266__backfill_tutor_scores_into_recall_log.sql";
  }

  @Test
  void backfilledScoreFourLeavesAGoodRecallLogWithoutAnswer()
      throws UnexpectedNoAccessRightException {
    Timestamp recordedAt = makeMe.aTimestamp().of(3, 8).please();
    MemoryTracker tracker = ownedTracker();
    seedScoredItem(tracker, 4, recordedAt);

    applyBackfill();

    List<RecallLog> logs = controller.getRecallLogs(tracker);
    assertThat(logs, hasSize(1));
    RecallLog log = logs.get(0);
    assertThat(log.getProductOutcome(), is(ProductOutcome.GOOD));
    assertThat(log.getAnswerId(), nullValue());
    assertThat(log.getElapsedHours(), nullValue());
    assertThat(log.getRecordedAt(), equalTo(recordedAt));
    assertThat(log.getMemoryTrackerId(), equalTo(tracker.getId()));
  }

  @ParameterizedTest
  @ValueSource(ints = {5, 3, 2, 1, 0})
  void backfilledScoreLeavesMappedRecallLog(int score) throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker();
    seedScoredItem(tracker, score, makeMe.aTimestamp().please());

    applyBackfill();

    assertThat(
        controller.getRecallLogs(tracker).get(0).getProductOutcome(),
        is(CommissionedLearningSessionFeedbackScheduling.productOutcomeForScore(score)));
  }

  @Test
  void alreadyLoggedTutorScoreIsNotBackfilledAgain() throws UnexpectedNoAccessRightException {
    Timestamp recordedAt = makeMe.aTimestamp().of(3, 8).please();
    MemoryTracker tracker = ownedTracker();
    seedScoredItem(tracker, 4, recordedAt);
    makeMe
        .aRecallLogFor(tracker)
        .recordedAt(recordedAt)
        .productOutcome(ProductOutcome.GOOD)
        .please();

    applyBackfill();

    assertThat(controller.getRecallLogs(tracker), hasSize(1));
  }

  @Test
  void backfillIsIdempotentWhenRunTwice() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker();
    seedScoredItem(tracker, 4, makeMe.aTimestamp().please());

    applyBackfill();
    applyBackfill();

    assertThat(controller.getRecallLogs(tracker), hasSize(1));
  }

  private void seedScoredItem(MemoryTracker tracker, int score, Timestamp recordedAt) {
    makeMe
        .aSessionItem()
        .learningSession(
            makeMe
                .aLearningSession()
                .by(currentUser.getUser())
                .forNotebook(tracker.getNote().getNotebook())
                .recordedAt(recordedAt)
                .please())
        .memoryTracker(tracker)
        .feedbackScore(score)
        .feedbackRecordedAt(recordedAt)
        .please();
  }
}
