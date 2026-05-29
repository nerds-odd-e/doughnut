package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AssimilationNextDTO;
import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
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

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class Assimilating {
    @Test
    void assimilating() {
      Note n = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      assertThat(n.getId(), notNullValue());
      List<NoteRealm> memoryTrackerWithRecallSettings = controller.assimilating("Asia/Shanghai");
      assertThat(memoryTrackerWithRecallSettings, hasSize(1));
      assertThat(memoryTrackerWithRecallSettings.get(0).getNote().getId(), equalTo(n.getId()));
    }

    @Test
    void notLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.assimilating("Asia/Shanghai"));
    }

    @Test
    void shouldHandleInvalidTimezoneByUsingUTC() {
      Note n = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      assertThat(n.getId(), notNullValue());
      List<NoteRealm> memoryTrackerWithRecallSettings = controller.assimilating("Etc/Unknown");
      assertThat(memoryTrackerWithRecallSettings, hasSize(1));
      assertThat(memoryTrackerWithRecallSettings.get(0).getNote().getId(), equalTo(n.getId()));
    }
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

      assertThat(controller.assimilating("Asia/Shanghai"), empty());

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextNoteId(), equalTo(note2.getId()));
      assertThat(result.getCounts().getDueCount(), equalTo(0));
    }

    @Test
    void subscriptionNoteBeforeOwnedNote() {
      User user = currentUser.getUser();
      User notebookOwner = makeMe.aUser().please();
      Notebook subscribedNotebook = makeMe.aNotebook().creatorAndOwner(notebookOwner).please();
      Note subscriptionNote = makeMe.aNote().notebook(subscribedNotebook).please();
      makeMe.aNote().notebookOwnedBy(user).please();
      makeMe.aSubscription().forNotebook(subscribedNotebook).forUser(user).daily(1).please();
      makeMe.refresh(user);

      AssimilationNextDTO result = controller.next("Asia/Shanghai");
      assertThat(result.getNextNoteId(), equalTo(subscriptionNote.getId()));
    }

    @Test
    void returnsNullWhenNoNotesLeft() {
      assertThat(controller.next("Asia/Shanghai").getNextNoteId(), nullValue());
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
