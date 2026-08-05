package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRecallInfo;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerNoteInfoTests extends ControllerTestBase {
  @Autowired NoteController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getNoteInfo(note));
  }

  @Test
  void shouldReturnTheNoteInfoIfHavingReadingAuth() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    makeMe.aSubscription().forUser(currentUser.getUser()).forNotebook(note.getNotebook()).please();
    makeMe.refresh(currentUser.getUser());
    assertThat(controller.getNoteInfo(note), notNullValue());
  }

  @Test
  void shouldIncludeSkippedMemoryTrackersInNoteInfo() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    makeMe.aMemoryTrackerFor(note).please();
    makeMe.aMemoryTrackerFor(note).spelling().removedFromTracking().please();

    NoteRecallInfo noteRecallInfo = controller.getNoteInfo(note);
    assertThat(noteRecallInfo.getMemoryTrackers(), hasSize(2));
    assertThat(
        noteRecallInfo.getMemoryTrackers(), hasItem(hasProperty("removedFromTracking", is(true))));
  }
}
