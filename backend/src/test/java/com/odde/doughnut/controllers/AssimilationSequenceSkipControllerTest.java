package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.AssimilationSequenceSkipRequestDTO;
import com.odde.doughnut.entities.AssimilationSequenceSkip;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.AssimilationSequenceSkipRepository;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.sql.Timestamp;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class AssimilationSequenceSkipControllerTest extends ControllerTestBase {
  @Autowired AssimilationSequenceSkipController controller;
  @Autowired AssimilationSequenceSkipRepository skipRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void notLoggedIn() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class,
        () -> controller.create(new AssimilationSequenceSkipRequestDTO()));
  }

  @Test
  void insertsSkipRowForNote() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    Timestamp now = makeMe.aTimestamp().of(1, 8).fromShanghai().please();
    testabilitySettings.timeTravelTo(now);

    AssimilationSequenceSkipRequestDTO request = new AssimilationSequenceSkipRequestDTO();
    request.noteId = note.getId();

    AssimilationSequenceSkip skip = controller.create(request);

    assertThat(skip.getId(), notNullValue());
    assertThat(skip.getNote().getId(), equalTo(note.getId()));
    assertThat(skip.getPropertyKey(), equalTo(""));
    assertThat(skip.getSkippedAt(), equalTo(now));
  }

  @Test
  void duplicateSkipIsIdempotent() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    AssimilationSequenceSkipRequestDTO request = new AssimilationSequenceSkipRequestDTO();
    request.noteId = note.getId();

    AssimilationSequenceSkip first = controller.create(request);
    AssimilationSequenceSkip second = controller.create(request);

    assertThat(second.getId(), equalTo(first.getId()));
  }

  @Test
  void deleteNotLoggedIn() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class,
        () -> controller.deleteAssimilationSequenceSkip(new AssimilationSequenceSkipRequestDTO()));
  }

  @Test
  void deletesSkipRowWithoutCreatingATracker() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.anAssimilationSequenceSkipFor(note).please();
    AssimilationSequenceSkipRequestDTO request = new AssimilationSequenceSkipRequestDTO();
    request.noteId = note.getId();

    controller.deleteAssimilationSequenceSkip(request);

    assertThat(
        skipRepository.findByUserAndNoteAndPropertyKey(currentUser.getUser(), note, ""),
        is(Optional.empty()));
    assertThat(
        memoryTrackerRepository.findByUserAndNote(currentUser.getUser().getId(), note.getId()),
        empty());
  }

  @Test
  void deletesPropertySkipRow() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.anAssimilationSequenceSkipFor(note).propertyKey("topic").please();
    AssimilationSequenceSkipRequestDTO request = new AssimilationSequenceSkipRequestDTO();
    request.noteId = note.getId();
    request.propertyKey = "topic";

    controller.deleteAssimilationSequenceSkip(request);

    assertThat(
        skipRepository.findByUserAndNoteAndPropertyKey(currentUser.getUser(), note, "topic"),
        is(Optional.empty()));
  }
}
