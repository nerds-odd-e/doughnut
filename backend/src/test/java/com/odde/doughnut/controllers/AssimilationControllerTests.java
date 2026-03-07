package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.InitialInfo;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
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
      Note n = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      assertThat(n.getId(), notNullValue());
      List<Note> memoryTrackerWithRecallSettings = controller.assimilating("Asia/Shanghai");
      assertThat(memoryTrackerWithRecallSettings, hasSize(1));
    }

    @Test
    void notLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.assimilating("Asia/Shanghai"));
    }

    @Test
    void shouldHandleInvalidTimezoneByUsingUTC() {
      Note n = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      assertThat(n.getId(), notNullValue());
      List<Note> memoryTrackerWithRecallSettings = controller.assimilating("Etc/Unknown");
      assertThat(memoryTrackerWithRecallSettings, hasSize(1));
    }
  }

  @Nested
  class CreateAssimilationPoint {
    @Test
    void create() {
      currentUser.setUser(null);
      InitialInfo info = new InitialInfo();
      assertThrows(ResponseStatusException.class, () -> controller.assimilate(info));
    }

    @Test
    void shouldCreateTwoMemoryTrackersWhenRememberSpellingIsTrue() {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getRecallSetting().setRememberSpelling(true);
      noteRepository.save(note);

      InitialInfo initialInfo = new InitialInfo();
      initialInfo.noteId = note.getId();

      controller.assimilate(initialInfo);

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
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      InitialInfo initialInfo = new InitialInfo();
      initialInfo.noteId = note.getId();

      List<MemoryTracker> result = controller.assimilate(initialInfo);

      assertThat(result, empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void shouldAddOnlySpellingTrackerWhenAddSpellingOnlyAndNoteHasTrackersButNoSpelling() {
      Note note = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      note.getRecallSetting().setRememberSpelling(true);
      noteRepository.save(note);
      makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      InitialInfo initialInfo = new InitialInfo();
      initialInfo.noteId = note.getId();
      initialInfo.addSpellingOnly = true;

      List<MemoryTracker> result = controller.assimilate(initialInfo);

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getSpelling(), equalTo(true));
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(2));
    }
  }
}
