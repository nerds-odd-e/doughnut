package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.AssimilationRequestDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class AssimilationControllerAssimilateTests extends ControllerTestBase {
  @Autowired private MemoryTrackerRepository memoryTrackerRepository;
  @Autowired private AssimilationSequenceSkipRepository skipRepository;
  @Autowired AssimilationController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
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
    void ordinaryAssimilateCreatesOnlyUnderstandingTracker() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      List<MemoryTracker> result =
          controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.UNDERSTANDING));
    }

    @Test
    void assimilatingSkippedNoteDeletesMatchingSkipRow() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.anAssimilationSequenceSkipFor(note).please();

      controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));

      assertThat(
          skipRepository.findByUserAndNoteAndPropertyKey(currentUser.getUser(), note, ""),
          is(Optional.empty()));
    }

    @Test
    void assimilatingSkippedPropertyDeletesMatchingSkipRow() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.anAssimilationSequenceSkipFor(note).propertyKey("a part of").please();

      controller.assimilate(
          AssimilationControllerTestSupport.assimilatePropertyRequest(note, "a part of"));

      assertThat(
          skipRepository.findByUserAndNoteAndPropertyKey(currentUser.getUser(), note, "a part of"),
          is(Optional.empty()));
    }

    @Test
    void shouldReturnEmptyWhenNoteAlreadyHasMemoryTrackers() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).please();

      List<MemoryTracker> result =
          controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));

      assertThat(result, empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void assimilateLeavesLastRecallUnsetAndDueAtAssimilatedAt() {
      Timestamp now = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(now);
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      MemoryTracker tracker =
          controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note)).get(0);

      assertThat(tracker.getAssimilatedAt(), equalTo(now));
      assertThat(tracker.getLastRecalledAt(), nullValue());
      assertThat(tracker.getNextRecallAt(), equalTo(now));
    }

    @Test
    void shouldCreatePropertyTrackerWhenPropertyKeyProvided() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      List<MemoryTracker> result =
          controller.assimilate(
              AssimilationControllerTestSupport.assimilatePropertyRequest(note, "a part of"));

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getPropertyKey(), equalTo("a part of"));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.UNDERSTANDING));
    }

    @Test
    void assimilateDoesNotCreateTrackerRemovedFromRecall() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      assertThat(
          controller
              .assimilate(AssimilationControllerTestSupport.assimilateRequest(note))
              .get(0)
              .getRemovedFromTracking(),
          equalTo(false));
    }

    @Test
    void shouldReturnEmptyWhenPropertyTrackerAlreadyExists() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      AssimilationRequestDTO request =
          AssimilationControllerTestSupport.assimilatePropertyRequest(note, "a part of");
      controller.assimilate(request);

      assertThat(controller.assimilate(request), empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void shouldCoexistNoteLevelAndPropertyTrackersOnSameNote() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));
      controller.assimilate(
          AssimilationControllerTestSupport.assimilatePropertyRequest(note, "a part of"));

      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(2));
    }
  }
}
