package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NotebookUpdateRequest;
import com.odde.doughnut.entities.DisplayName;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

class NotebookUpdateControllerTest extends NotebookControllerTestBase {

  @Test
  void shouldNotBeAbleToUpdateNotebookThatBelongsToOtherUser() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.updateNotebook(note.getNotebook(), new NotebookUpdateRequest()));
  }

  @Test
  void shouldPersistDescriptionOnUpdate() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    var request = new NotebookUpdateRequest();
    request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
    request.setDescription("Notebook blurb");
    controller.updateNotebook(note.getNotebook(), request);
    assertThat(note.getNotebook().getDescription(), equalTo("Notebook blurb"));
  }

  @Test
  void shouldClearDescriptionWhenEmptyString() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    note.getNotebook().setDescription("was set");
    var setRequest = new NotebookUpdateRequest();
    setRequest.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
    setRequest.setDescription("");
    controller.updateNotebook(note.getNotebook(), setRequest);
    assertThat(note.getNotebook().getDescription(), nullValue());
  }

  @Test
  void shouldLeaveDescriptionUnchangedWhenDescriptionOmitted()
      throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    note.getNotebook().setDescription("unchanged");
    var request = new NotebookUpdateRequest();
    request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
    controller.updateNotebook(note.getNotebook(), request);
    assertThat(note.getNotebook().getDescription(), equalTo("unchanged"));
  }

  @Test
  void shouldPersistNameOnUpdate() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    note.getNotebook().setName(new DisplayName("Old Title"));
    var request = new NotebookUpdateRequest();
    request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
    request.setName("  New Title  ");
    controller.updateNotebook(note.getNotebook(), request);
    assertThat(note.getNotebook().getName(), equalTo("New Title"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\u3000"})
  void shouldRejectEmptyOrWhitespaceNameOnUpdate(String name) {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    var request = new NotebookUpdateRequest();
    request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
    request.setName(name);
    assertThrows(
        ResponseStatusException.class,
        () -> controller.updateNotebook(note.getNotebook(), request));
  }

  @Test
  void shouldLeaveNameUnchangedWhenNameOmitted() throws UnexpectedNoAccessRightException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    note.getNotebook().setName(new DisplayName("Original Name"));
    var request = new NotebookUpdateRequest();
    request.setNotebookSettings(copyNotebookSettings(note.getNotebook()));
    controller.updateNotebook(note.getNotebook(), request);
    assertThat(note.getNotebook().getName(), equalTo("Original Name"));
  }
}
