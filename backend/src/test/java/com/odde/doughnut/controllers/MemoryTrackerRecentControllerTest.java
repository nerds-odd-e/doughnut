package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerRecentControllerTest extends MemoryTrackerControllerTestBase {

  @Nested
  class GetRecentMemoryTrackers {
    @Test
    void shouldReturnEmptyListWhenNoMemoryTrackers() {
      assertThat(controller.getRecentMemoryTrackers(), empty());
    }

    @Test
    void shouldReturnMemoryTrackersForCurrentUser() {
      MemoryTracker tracker1 = ownedTracker();
      MemoryTracker tracker2 = ownedTracker();

      assertThat(controller.getRecentMemoryTrackers(), containsInAnyOrder(tracker1, tracker2));
    }

    @Test
    void shouldNotReturnMemoryTrackersFromOtherUsers() {
      makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
      assertThat(controller.getRecentMemoryTrackers(), empty());
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.getRecentMemoryTrackers());
    }

    @Test
    void shouldExcludeMemoryTrackersForDeletedNotes() {
      Note activeNote = ownedNote();
      Note deletedNote = ownedNote();
      MemoryTracker activeTracker = ownedTracker(activeNote);
      ownedTracker(deletedNote);

      noteService.destroy(
          deletedNote, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

      assertThat(controller.getRecentMemoryTrackers(), contains(activeTracker));
    }
  }

  @Nested
  class GetRecentlyRecalled {
    @Test
    void shouldReturnEmptyListWhenNoRecalled() {
      assertThat(controller.getRecentlyRecalled(), empty());
    }

    @Test
    void shouldReturnRecentlyRecalledForCurrentUser() {
      MemoryTracker tracker1 = ownedTracker();
      MemoryTracker tracker2 = ownedTracker();
      controller.markAsRecalled(tracker1, true);
      controller.markAsRecalled(tracker2, true);

      assertThat(controller.getRecentlyRecalled(), containsInAnyOrder(tracker1, tracker2));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.getRecentlyRecalled());
    }

    @Test
    void shouldExcludeMemoryTrackersForDeletedNotes() {
      Note activeNote = ownedNote();
      Note deletedNote = ownedNote();
      MemoryTracker activeTracker = ownedTracker(activeNote);
      MemoryTracker deletedTracker = ownedTracker(deletedNote);

      controller.markAsRecalled(activeTracker, true);
      controller.markAsRecalled(deletedTracker, true);

      noteService.destroy(
          deletedNote, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

      assertThat(controller.getRecentlyRecalled(), contains(activeTracker));
    }
  }
}
