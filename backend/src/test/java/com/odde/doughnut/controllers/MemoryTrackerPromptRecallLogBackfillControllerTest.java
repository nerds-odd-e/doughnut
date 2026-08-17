package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ProductOutcome;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryTrackerPromptRecallLogBackfillControllerTest
    extends RecallLogBackfillControllerTestBase {
  @Override
  protected String migrationSql() {
    return "db/migration/V300000265__backfill_prompt_answers_into_recall_log.sql";
  }

  @Test
  void backfilledCorrectAnswerLeavesAGoodRecallLogLinkedToTheAnswer()
      throws UnexpectedNoAccessRightException {
    Timestamp answeredAt = makeMe.aTimestamp().of(3, 8).please();
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    RecallPrompt prompt =
        makeMe
            .aRecallPrompt()
            .withMcqForNote(note)
            .forMemoryTracker(tracker)
            .answerChoiceIndex(0)
            .answerTimestamp(answeredAt)
            .please();

    applyBackfill();

    List<RecallLog> logs = controller.getRecallLogs(tracker);
    assertThat(logs, hasSize(1));
    RecallLog log = logs.get(0);
    assertThat(log.getProductOutcome(), is(ProductOutcome.GOOD));
    assertThat(log.getAnswerId(), equalTo(prompt.getAnswer().getId()));
    assertThat(log.getElapsedHours(), nullValue());
    assertThat(log.getRecordedAt(), equalTo(prompt.getAnswer().getCreatedAt()));
    assertThat(log.getMemoryTrackerId(), equalTo(tracker.getId()));
  }

  @Test
  void backfilledIncorrectAnswerLeavesAnAgainRecallLog() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    makeMe
        .aRecallPrompt()
        .withMcqForNote(note)
        .forMemoryTracker(tracker)
        .answerChoiceIndex(1)
        .please();

    applyBackfill();

    assertThat(
        controller.getRecallLogs(tracker).get(0).getProductOutcome(), is(ProductOutcome.AGAIN));
  }

  @Test
  void backfilledOverlapAnswerDoesNotWriteARecallLog() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker();
    makeMe.aRecallPrompt().forMemoryTracker(tracker).overlap().please();

    applyBackfill();

    assertThat(controller.getRecallLogs(tracker), empty());
  }

  @Test
  void backfilledAccidentalMatchWritesARecallLog() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker();
    makeMe.aRecallPrompt().forMemoryTracker(tracker).accidentalMatch().please();

    applyBackfill();

    assertThat(controller.getRecallLogs(tracker), hasSize(1));
  }

  @Test
  void alreadyLoggedAnswerIsNotBackfilledAgain() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    makeMe
        .aRecallPrompt()
        .withMcqForNote(note)
        .forMemoryTracker(tracker)
        .answerChoiceIndex(0)
        .please();

    applyBackfill();
    applyBackfill();

    assertThat(controller.getRecallLogs(tracker), hasSize(1));
  }
}
