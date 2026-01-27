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
  class CreateInitialReviewPoint {
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
    void shouldNotCreateSpellingMemoryTrackerForLinkNoteEvenWhenRememberSpellingIsTrue() {
      Note parentNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note targetNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      Note linkNote = makeMe.aRelation().between(parentNote, targetNote).please();
      linkNote.getRecallSetting().setRememberSpelling(true);
      noteRepository.save(linkNote);

      InitialInfo initialInfo = new InitialInfo();
      initialInfo.noteId = linkNote.getId();

      controller.assimilate(initialInfo);

      List<MemoryTracker> memoryTrackers =
          memoryTrackerRepository.findLast100ByUser(currentUser.getUser().getId());

      long trackersForLinkNote =
          memoryTrackers.stream()
              .filter(mt -> mt.getNote().getId().equals(linkNote.getId()))
              .count();

      assertThat(trackersForLinkNote, equalTo(1L));
      assertThat(
          memoryTrackers.stream()
              .filter(mt -> mt.getNote().getId().equals(linkNote.getId()))
              .filter(MemoryTracker::getSpelling)
              .count(),
          equalTo(0L));
    }
  }
}
