package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AssimilationNextDTO;
import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.NotePropertyIndexService;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class AssimilationControllerTests extends ControllerTestBase {
  @Autowired private NoteRepository noteRepository;
  @Autowired private MemoryTrackerRepository memoryTrackerRepository;
  @Autowired AssimilationController controller;
  @Autowired NotePropertyIndexService notePropertyIndexService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class Next {
    @Test
    void returnsNextNoteIdPastDailyCap() {
      User user = currentUser.getUser();
      makeMe.theUser(user).dailyAssimilationCount(1).please();
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);

      Note note1 = makeMe.aNote("note1").notebookOwnedBy(user).please();
      Note note2 = makeMe.aNote("note2").notebookOwnedBy(user).please();
      makeMe.aMemoryTrackerFor(note1).by(user).assimilatedAt(day1).please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note2.getId()));
      assertThat(result.getCounts().getDueCount(), equalTo(0));
    }

    @Test
    void returnsOwnedNoteWhenSubscriptionDailyCapReached() {
      User user = currentUser.getUser();
      User notebookOwner = makeMe.aUser().please();
      Notebook subscribedNotebook = makeMe.aNotebook().creatorAndOwner(notebookOwner).please();
      Note subscriptionNote = makeMe.aNote("sub").notebook(subscribedNotebook).please();
      Note ownedNote = makeMe.aNote("owned").notebookOwnedBy(user).please();
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
      User user = currentUser.getUser();
      makeMe.aNote("note1").notebookOwnedBy(user).please();
      makeMe.aNote("note2").notebookOwnedBy(user).please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getCounts().getDueCount(), equalTo(2));
      assertThat(result.getCounts().getAssimilatedCountOfTheDay(), equalTo(0));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(2));
    }

    @Test
    void notLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.next("Asia/Shanghai"));
    }

    @Test
    void returns_next_property_key_for_untracked_example_of() {
      User user = currentUser.getUser();
      Timestamp day1 = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(day1);
      Note note =
          makeMe
              .aNote()
              .notebookOwnedBy(user)
              .content("---\nexample of: \"[[Word]]\"\n---\n\nbody")
              .please();
      notePropertyIndexService.refreshForNote(note);
      makeMe.aMemoryTrackerFor(note).by(user).assimilatedAt(day1).please();

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextUnit().getNoteId(), equalTo(note.getId()));
      assertThat(result.getNextUnit().getPropertyKey(), equalTo("example of"));
      assertThat(result.getCounts().getTotalUnassimilatedCount(), equalTo(1));
    }
  }

  @Nested
  class CreateAssimilationPoint {
    @Test
    void create() {
      currentUser.setUser(null);
      AssimilationRequestDTO info = new AssimilationRequestDTO();
      assertThrows(ResponseStatusException.class, () -> controller.assimilate(info));
    }

    @Test
    void shouldCreateTwoMemoryTrackersWhenRememberSpellingIsTrue() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      note.getRecallSetting().setRememberSpelling(true);
      noteRepository.save(note);

      AssimilationRequestDTO request = new AssimilationRequestDTO();
      request.noteId = note.getId();

      controller.assimilate(request);

      List<MemoryTracker> memoryTrackers =
          memoryTrackerRepository.findLast100ByUser(currentUser.getUser().getId());
      assertThat(
          memoryTrackers.stream().filter(mt -> mt.getNote().getId().equals(note.getId())).count(),
          equalTo(2L));
      assertThat(memoryTrackers.stream().filter(mt -> mt.getSpelling()).count(), equalTo(1L));
      assertThat(memoryTrackers.stream().filter(mt -> !mt.getSpelling()).count(), equalTo(1L));
    }

    @Test
    void shouldReturnEmptyWhenNoteAlreadyHasMemoryTrackers() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      AssimilationRequestDTO request = new AssimilationRequestDTO();
      request.noteId = note.getId();

      List<MemoryTracker> result = controller.assimilate(request);

      assertThat(result, empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void shouldAddOnlySpellingTrackerWhenAddSpellingOnlyAndNoteHasTrackersButNoSpelling() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      note.getRecallSetting().setRememberSpelling(true);
      noteRepository.save(note);
      makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      AssimilationRequestDTO request = new AssimilationRequestDTO();
      request.noteId = note.getId();

      List<MemoryTracker> result = controller.assimilate(request);

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getSpelling(), equalTo(true));
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(2));
    }
  }
}
