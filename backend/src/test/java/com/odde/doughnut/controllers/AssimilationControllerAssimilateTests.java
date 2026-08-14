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
    void assimilatingCommissionedOnlyNoteCreatesUnderstandingAndLeavesCommissioned() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).commissioned().please();

      List<MemoryTracker> result =
          controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note));

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.UNDERSTANDING));
      assertThat(
          memoryTrackerRepository
              .findByUserAndNote(currentUser.getUser().getId(), note.getId())
              .stream()
              .map(MemoryTracker::getType)
              .toList(),
          containsInAnyOrder(MemoryTrackerType.UNDERSTANDING, MemoryTrackerType.COMMISSIONED));
    }

    @Test
    void assimilatingAsCommissionedCreatesOnlyCommissionedTracker() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      List<MemoryTracker> result =
          controller.assimilate(
              AssimilationControllerTestSupport.assimilateCommissionedRequest(note));

      assertThat(result, hasSize(1));
      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.COMMISSIONED));
      assertThat(result.get(0).isNoteLevelTracker(), equalTo(true));
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
    }

    @Test
    void assimilatingAsCommissionedOnSkippedNoteLeavesSkipRowAndStaysOutOfNext() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.anAssimilationSequenceSkipFor(note).please();

      List<MemoryTracker> result =
          controller.assimilate(
              AssimilationControllerTestSupport.assimilateCommissionedRequest(note));

      assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.COMMISSIONED));
      assertThat(
          skipRepository.findByUserAndNoteAndPropertyKey(currentUser.getUser(), note, ""),
          is(not(Optional.empty())));
      assertThat(controller.next("Asia/Shanghai").getNextUnit(), nullValue());
    }

    @Test
    void
        assimilatingAsCommissionedWhenUnderstandingExistsCreatesCommissionedAndLeavesUnderstanding() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).please();

      controller.assimilate(AssimilationControllerTestSupport.assimilateCommissionedRequest(note));

      assertThat(
          memoryTrackerRepository
              .findByUserAndNote(currentUser.getUser().getId(), note.getId())
              .stream()
              .map(MemoryTracker::getType)
              .toList(),
          containsInAnyOrder(MemoryTrackerType.UNDERSTANDING, MemoryTrackerType.COMMISSIONED));
    }

    @Test
    void assimilatingAsCommissionedWithPropertyKeyReturnsEmpty() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      AssimilationRequestDTO request =
          AssimilationControllerTestSupport.assimilateCommissionedRequest(note);
      request.propertyKey = "a part of";

      assertThat(controller.assimilate(request), empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          empty());
    }

    @Test
    void assimilatingAsCommissionedWhenCommissionedExistsReturnsEmpty() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(note).commissioned().please();

      assertThat(
          controller.assimilate(
              AssimilationControllerTestSupport.assimilateCommissionedRequest(note)),
          empty());
      assertThat(
          memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
          hasSize(1));
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
    void shouldSetAssimilatedAtAndLastRecalledAt() {
      Timestamp now = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
      testabilitySettings.timeTravelTo(now);
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

      MemoryTracker tracker =
          controller.assimilate(AssimilationControllerTestSupport.assimilateRequest(note)).get(0);

      assertThat(tracker.getAssimilatedAt(), equalTo(now));
      assertThat(tracker.getLastRecalledAt(), equalTo(now));
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
    void shouldCreatePropertyTrackerRemovedFromTrackingWhenSkipMemoryTracking() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      AssimilationRequestDTO request =
          AssimilationControllerTestSupport.assimilatePropertyRequest(note, "a part of");
      request.skipMemoryTracking = true;

      assertThat(controller.assimilate(request).get(0).getRemovedFromTracking(), equalTo(true));
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
