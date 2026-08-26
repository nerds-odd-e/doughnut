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

class AssimilationControllerAssimilateSpellingTests extends ControllerTestBase {
  @Autowired private MemoryTrackerRepository memoryTrackerRepository;
  @Autowired private AssimilationSequenceSkipRepository skipRepository;
  @Autowired AssimilationController controller;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void assimilatingAsSpellingOnSkippedNoteLeavesSkipRowAndStaysOutOfNext() {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.anAssimilationSequenceSkipFor(note).please();

    List<MemoryTracker> result =
        controller.assimilate(AssimilationControllerTestSupport.assimilateSpellingRequest(note));

    assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.SPELLING));
    assertThat(
        skipRepository.findByUserAndNoteAndPropertyKey(currentUser.getUser(), note, ""),
        is(not(Optional.empty())));
    assertThat(controller.next("Asia/Shanghai").getNextUnit(), nullValue());
  }

  @Test
  void assimilatingAsSpellingCreatesOnlySpellingTracker() {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();

    List<MemoryTracker> result =
        controller.assimilate(AssimilationControllerTestSupport.assimilateSpellingRequest(note));

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getType(), equalTo(MemoryTrackerType.SPELLING));
    assertThat(result.get(0).isNoteLevelTracker(), equalTo(true));
    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
        hasSize(1));
  }

  @Test
  void assimilatingAsSpellingWhenUnderstandingExistsCreatesSpellingAndLeavesUnderstanding() {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aMemoryTrackerFor(note).please();

    controller.assimilate(AssimilationControllerTestSupport.assimilateSpellingRequest(note));

    assertThat(
        memoryTrackerRepository
            .findByUserAndNote(currentUser.getUser().getId(), note.getId())
            .stream()
            .map(MemoryTracker::getType)
            .toList(),
        containsInAnyOrder(MemoryTrackerType.UNDERSTANDING, MemoryTrackerType.SPELLING));
  }

  @Test
  void assimilatingAsSpellingWithPropertyKeyReturnsEmpty() {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    AssimilationRequestDTO request =
        AssimilationControllerTestSupport.assimilateSpellingRequest(note);
    request.propertyKey = "a part of";

    assertThat(controller.assimilate(request), empty());
    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
        empty());
  }

  @Test
  void assimilatingAsSpellingWhenSpellingExistsReturnsEmpty() {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aMemoryTrackerFor(note).spelling().please();

    assertThat(
        controller.assimilate(AssimilationControllerTestSupport.assimilateSpellingRequest(note)),
        empty());
    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
        hasSize(1));
  }

  @Test
  void assimilatingSpellingOnlyNoteCreatesUnderstandingAndLeavesSpelling() {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aMemoryTrackerFor(note).spelling().please();

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
        containsInAnyOrder(MemoryTrackerType.UNDERSTANDING, MemoryTrackerType.SPELLING));
  }
}
