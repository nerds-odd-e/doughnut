package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.dto.AssimilationRequestDTO;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AssimilationControllerAssimilateCommissionedTests extends ControllerTestBase {
  @Autowired private MemoryTrackerRepository memoryTrackerRepository;
  @Autowired private AssimilationSequenceSkipRepository skipRepository;
  @Autowired AssimilationController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
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
}
