package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AssimilationNextDTO;
import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.services.NotePropertyIndexService;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class AssimilationControllerTests extends ControllerTestBase {
  @Autowired private MemoryTrackerRepository memoryTrackerRepository;
  @Autowired AssimilationController controller;
  @Autowired NotePropertyIndexService notePropertyIndexService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  Note ownedNote(String title) {
    return makeMe.aNote(title).notebookOwnedBy(currentUser.getUser()).please();
  }

  AssimilationRequestDTO assimilateRequest(Note note) {
    AssimilationRequestDTO request = new AssimilationRequestDTO();
    request.noteId = note.getId();
    return request;
  }

  AssimilationRequestDTO assimilatePropertyRequest(Note note, String propertyKey) {
    AssimilationRequestDTO request = assimilateRequest(note);
    request.propertyKey = propertyKey;
    return request;
  }

  @Nested
  class Next {
    @Test
    void returnsOwnedNoteWhenSubscriptionDailyCapReached() {
      User user = currentUser.getUser();
      Notebook subscribedNotebook =
          makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      Note subscriptionNote = makeMe.aNote("sub").notebook(subscribedNotebook).please();
      Note ownedNote = ownedNote("owned");
      makeMe.aSubscription().forNotebook(subscribedNotebook).forUser(user).daily(1).please();
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);
      makeMe.aMemoryTrackerFor(subscriptionNote).by(user).assimilatedAt(day1).please();
      makeMe.refresh(user);

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(ownedNote.getId()));
    }

    @Test
    void returnsNullWhenNoNotesLeft() {
      assertThat(controller.next("Asia/Shanghai").getNextUnit(), nullValue());
    }

    @Test
    void countsAreCorrect() {
      ownedNote("note1");
      ownedNote("note2");

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getCounts().getDueCount(), equalTo(2));
      assertThat(result.getCounts().getAssimilatedCountOfTheDay(), equalTo(0));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    }

    @Test
    void commissionedOnlyNoteStillAppearsInOrdinaryAssimilationQueue() {
      Note note = ownedNote("commissioned-only");
      makeMe.aMemoryTrackerFor(note).commissioned().please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note.getId()));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    }

    @Test
    void assimilatedCountOfTheDayExcludesCommissionedTrackers() {
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);
      Note note = ownedNote("commissioned-today");
      makeMe.aMemoryTrackerFor(note).commissioned().assimilatedAt(day1).please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getCounts().getAssimilatedCountOfTheDay(), equalTo(0));
    }

    @Test
    void notLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.next("Asia/Shanghai"));
    }

    @Test
    void returns_next_property_key_for_untracked_example_of() {
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);
      Note note =
          makeMe
              .aNote()
              .notebookOwnedBy(currentUser.getUser())
              .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
              .please();
      notePropertyIndexService.refreshForNote(note);
      makeMe.aMemoryTrackerFor(note).assimilatedAt(day1).please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note.getId()));
      assertThat(result.getNextUnit().getPropertyKey(), equalTo("example of"));
    }
  }

  @Nested
  class CreateAssimilationPoint {
    @Test
    void notLoggedIn() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.assimilate(new AssimilationRequestDTO()));
    }

    @Test
    void shouldCreateTwoMemoryTrackersWhenRememberSpellingIsTrue() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).rememberSpelling().please();

      List<MemoryTracker> result = controller.assimilate(assimilateRequest(note));

      assertThat(result, hasSize(2));
      assertThat(result.stream().filter(MemoryTracker::isSpelling).count(), equalTo(1L));
    }

    @Test
    void understandingAndCommissionedTrackersCanCoexistOnSameNote() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).please();
      makeMe.aMemoryTrackerFor(note).commissioned().please();

      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(2));
    }

    @Test
    void shouldReturnEmptyWhenNoteAlreadyHasMemoryTrackers() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).please();

      List<MemoryTracker> result = controller.assimilate(assimilateRequest(note));

      assertThat(result, empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void shouldAddOnlySpellingTrackerWhenAddSpellingOnlyAndNoteHasTrackersButNoSpelling() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).rememberSpelling().please();
      makeMe.aMemoryTrackerFor(note).please();

      List<MemoryTracker> result = controller.assimilate(assimilateRequest(note));

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.SPELLING));
    }

    @Test
    void shouldSetAssimilatedAtAndLastRecalledAt() {
      Timestamp now = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(now);
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      MemoryTracker tracker = controller.assimilate(assimilateRequest(note)).get(0);

      assertThat(tracker.getAssimilatedAt(), equalTo(now));
      assertThat(tracker.getLastRecalledAt(), equalTo(now));
    }

    @Test
    void shouldCreatePropertyTrackerWhenPropertyKeyProvided() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      List<MemoryTracker> result =
          controller.assimilate(assimilatePropertyRequest(note, "a part of"));

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getPropertyKey(), equalTo("a part of"));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.UNDERSTANDING));
    }

    @Test
    void shouldCreatePropertyTrackerRemovedFromTrackingWhenSkipMemoryTracking() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      AssimilationRequestDTO request = assimilatePropertyRequest(note, "a part of");
      request.skipMemoryTracking = true;

      assertThat(controller.assimilate(request).get(0).getRemovedFromTracking(), equalTo(true));
    }

    @Test
    void shouldReturnEmptyWhenPropertyTrackerAlreadyExists() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      AssimilationRequestDTO request = assimilatePropertyRequest(note, "a part of");
      controller.assimilate(request);

      assertThat(controller.assimilate(request), empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void shouldCoexistNoteLevelAndPropertyTrackersOnSameNote() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      controller.assimilate(assimilateRequest(note));
      controller.assimilate(assimilatePropertyRequest(note, "a part of"));

      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(2));
    }
  }
}
