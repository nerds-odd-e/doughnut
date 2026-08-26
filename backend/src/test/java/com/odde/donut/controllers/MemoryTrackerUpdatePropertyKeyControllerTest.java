package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerUpdatePropertyKeyControllerTest extends MemoryTrackerControllerTestBase {

  @Test
  void shouldRenamePropertyKeyWhilePreservingStats() throws UnexpectedNoAccessRightException {
    Note note = ownedNote();
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(note).propertyKey("topic").afterNthStrictRecall(2).please();
    Integer recallCount = tracker.getRecallCount();
    Float stability = tracker.getStability();
    Timestamp nextRecallAt = tracker.getNextRecallAt();

    MemoryTracker result = controller.updatePropertyKey(tracker, renameTo("subject"));

    assertThat(result.getPropertyKey(), equalTo("subject"));
    assertThat(result.getRecallCount(), equalTo(recallCount));
    assertThat(result.getStability(), equalTo(stability));
    assertThat(result.getNextRecallAt(), equalTo(nextRecallAt));
  }

  @Test
  void shouldRejectRenameWhenPropertyKeyAlreadyTaken() {
    Note note = ownedNote();
    makeMe.aMemoryTrackerFor(note).propertyKey("subject").please();
    MemoryTracker tracker = makeMe.aMemoryTrackerFor(note).propertyKey("topic").please();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.updatePropertyKey(tracker, renameTo("subject")));

    assertThat(ex.getStatusCode(), equalTo(HttpStatus.CONFLICT));
  }

  @Test
  void shouldRejectRenameForNoteLevelTracker() {
    MemoryTracker tracker = ownedTracker();

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.updatePropertyKey(tracker, renameTo("topic")));

    assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldNotBeAbleToRenameOthersMemoryTracker() {
    MemoryTracker tracker =
        makeMe.aMemoryTrackerBy(makeMe.aUser().please()).propertyKey("topic").please();

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.updatePropertyKey(tracker, renameTo("subject")));
  }

  @Test
  void shouldRequireUserToBeLoggedIn() {
    currentUser.setUser(null);
    MemoryTracker tracker =
        makeMe.aMemoryTrackerBy(makeMe.aUser().please()).propertyKey("topic").please();

    assertThrows(
        ResponseStatusException.class,
        () -> controller.updatePropertyKey(tracker, renameTo("subject")));
  }
}
