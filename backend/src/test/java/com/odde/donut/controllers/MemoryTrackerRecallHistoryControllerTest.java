package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.RecallHistoryItem;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerRecallHistoryControllerTest extends MemoryTrackerControllerTestBase {

  @Test
  void pairsARecallLogWithThePromptThatSharesItsAnswer() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    RecallPrompt answered = answeredPromptFor(tracker, note);
    RecallLog log = makeMe.aRecallLogFor(tracker).answer(answered.getAnswer()).please();

    List<RecallHistoryItem> history = controller.getRecallHistory(tracker);

    assertThat(history, hasSize(1));
    assertThat(history.get(0).getRecallLog().getId(), equalTo(log.getId()));
    assertThat(history.get(0).getRecallPrompt().getId(), equalTo(answered.getId()));
  }

  @Test
  void justReviewLogHasNoPrompt() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker();
    RecallLog log = makeMe.aRecallLogFor(tracker).please();

    List<RecallHistoryItem> history = controller.getRecallHistory(tracker);

    assertThat(history, hasSize(1));
    assertThat(history.get(0).getRecallLog().getId(), equalTo(log.getId()));
    assertThat(history.get(0).getRecallPrompt(), nullValue());
  }

  @Test
  void unansweredPromptHasNoLog() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    RecallPrompt unanswered = promptFor(tracker, note);

    List<RecallHistoryItem> history = controller.getRecallHistory(tracker);

    assertThat(history, hasSize(1));
    assertThat(history.get(0).getRecallLog(), nullValue());
    assertThat(history.get(0).getRecallPrompt().getId(), equalTo(unanswered.getId()));
  }

  @Test
  void newestFirstByLogTimeThenPromptTime() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    Timestamp day1 = makeMe.aTimestamp().of(1, 8).please();
    Timestamp day2 = makeMe.aTimestamp().of(2, 8).please();
    RecallPrompt unanswered = promptFor(tracker, note);
    unanswered.setCreatedAt(day1);
    makeMe.entityPersister.save(unanswered);
    RecallLog laterLog = makeMe.aRecallLogFor(tracker).recordedAt(day2).please();

    List<RecallHistoryItem> history = controller.getRecallHistory(tracker);

    assertThat(history.get(0).getRecallLog().getId(), equalTo(laterLog.getId()));
    assertThat(history.get(1).getRecallPrompt().getId(), equalTo(unanswered.getId()));
  }

  @Test
  void emptyWhenTrackerHasNeitherLogsNorPrompts() throws UnexpectedNoAccessRightException {
    assertThat(controller.getRecallHistory(ownedTracker()), empty());
  }

  @Test
  void shouldNotBeAbleToGetRecallHistoryForOthersMemoryTracker() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getRecallHistory(memoryTracker));
  }

  @Test
  void shouldRequireUserToBeLoggedIn() {
    currentUser.setUser(null);
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
    assertThrows(ResponseStatusException.class, () -> controller.getRecallHistory(memoryTracker));
  }
}
