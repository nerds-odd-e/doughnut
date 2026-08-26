package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
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
      controller.markAsRecalled(tracker1, Grade.GOOD);
      controller.markAsRecalled(tracker2, Grade.GOOD);

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

      controller.markAsRecalled(activeTracker, Grade.GOOD);
      controller.markAsRecalled(deletedTracker, Grade.GOOD);

      noteService.destroy(
          deletedNote, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());

      assertThat(controller.getRecentlyRecalled(), contains(activeTracker));
    }
  }
}
